(ns fabulate.parsing-test
  (:use midje.sweet)
  (:require [fabulate.parsing :as parsing])
  (:require [instaparse.core :as insta]))

(defn wtree-count [wt]
  (if wt
    (+ 1 (wtree-count (:less wt)) (wtree-count (:more wt)))
    0))

(defn flatten-with [wt f]
  (if wt
    (concat (flatten-with (:less wt) f) [(f (:item wt))] (flatten-with (:more wt) f))
    []))

(defn count-is [n]
  (fn [wt]
    (= n (wtree-count wt))))

(defn tree-contains 
  ([expected-elements] 
    (tree-contains expected-elements :item))
  ([expected-elements f]
    (fn [wt]
      ((just expected-elements :in-any-order) (flatten-with wt f)))))

(facts "single choice"
       (parsing/parse :choice "abc") => {:type :choice :weight 1 :item "abc"}
       (parsing/parse :choice "\"abc def\"") => {:type :choice :weight 1 :item "abc def"}
       (parsing/parse :choice "123") => {:type :choice :weight 1 :item 123}
       (parsing/parse :choice "123.456") => {:type :choice :weight 1 :item 123.456}
       )

(facts "list of choices"
       (parsing/parse :choice "<red green blue>") => (contains {:type :list :wtree (count-is 3)})
       (parsing/parse :choice "<red green blue>") => (contains {:type :list :wtree (tree-contains ["red" "green" "blue"])})
       (parsing/parse :choice "<red \"green\\\" blue\">") => (contains {:type :list :wtree (tree-contains ["red" "green\" blue"])})
       (parsing/parse :choice "<1 2 3>") => (contains {:type :list :wtree (count-is 3)})
       (parsing/parse :choice "<1 2 3>") => (contains {:wtree (tree-contains [1 2 3])})
       (parsing/parse :choice "<1 2 3>") => (contains {:wtree (tree-contains [1 1 1] :weight)})
)

(facts "list of weighted choices"
       (parsing/parse :choice "<red green:70 blue>") => (contains {:wtree (tree-contains [1 1 70] :weight)})
       (parsing/parse :choice "<red:10 green:70 blue:20>") => (contains {:type :list :wtree (tree-contains ["red" "green" "blue"])})
       (parsing/parse :choice "<red:10 green:70 blue:20>") => (contains {:type :list :wtree (tree-contains [10 20 70] :weight)})
       (parsing/parse :choice "<1:20 2:30 3:40>") => (contains {:type :list :wtree (count-is 3)})
       (parsing/parse :choice "<1:20 2:30 3:40>") => (contains {:type :list :wtree (tree-contains [30 20 40] :weight)})
)
(defn range-contains 
  ([expected-elements]
    (range-contains expected-elements :item))
  ([expected-elements f] 
    (fn [r]
      ((just expected-elements :in-any-order) (map f r)))))

(facts "a range"
       (parsing/parse :choice "[1 20]") => (contains {:type :range :items (range-contains [1 20])})
       (parsing/parse :choice "[1 20]") => (contains {:type :range :items (range-contains [1 1] :weight)})
       (parsing/parse :choice "[1:100 20:50]") => (contains {:type :range :items (range-contains [1 20])})
       (parsing/parse :choice "[1:100 20:50]") => (contains {:type :range :items (range-contains [100 50] :weight)})
       (parsing/parse :choice "[1:100 20:50 50:10]") => (contains {:type :list :wtree (count-is 2)})
)

(facts "errors"
       (parsing/parse :choice "abc(def") => insta/failure?
       (parsing/parse :choice "abc def") => insta/failure?
       (parsing/parse :choice "1 2") => insta/failure?
       (parsing/parse :choice "") => insta/failure?
       )

(facts "non-ASCII character handling"
       (parsing/parse :choice "<abc åÄÖ 123>") => (contains {:type :list :wtree (tree-contains ["abc" "åÄÖ" 123])})
       (parsing/parse :choice "<üb.er lambda_λ>") => (contains {:type :list :wtree (tree-contains ["üb.er" "lambda_λ"])})
       )

(facts "regex"
       (parsing/parse :choice "/\\w\\/([a-f]{4}|\\d{4})/") => (contains {:type :regex :pattern #"\w/([a-f]{4}|\d{4})"}))

(facts "numbers"
       (parsing/parse :choice "123") => (contains {:item 123})
       (parsing/parse :choice "a123") => (contains {:item "a123"})
       (parsing/parse :choice "-123") => (contains {:item -123})
       (parsing/parse :choice "12a3bc") => (throws NumberFormatException #"12a3bc")
       (parsing/parse :choice "<123 \"abc\">") => (contains {:wtree (tree-contains [123 "abc"])})
       (parsing/parse :choice "a123.456") => (contains {:item "a123.456"})
       (parsing/parse :choice "123.456") => (contains {:item 123.456})
       (parsing/parse :choice "-123.456") => (contains {:item -123.456})
       (parsing/parse :choice "-12a3.456x") => (throws NumberFormatException #"-12a3.456x")
       (parsing/parse :choice "1.23E05") => (contains {:item 123000.0})
       )

(facts "comments"
       (:f (parsing/parse :field "f 123 #abc")) => (contains {:item 123})
       (:f (parsing/parse :field "f 123 # abc")) => (contains {:item 123})
       (:f (parsing/parse :field "f <123 \"# abc\" 456>")) => (contains {:wtree (tree-contains [123 "# abc" 456])})
       (:f (parsing/parse :field "f <123 456> # abc")) => (contains {:wtree (tree-contains [123 456])})
       )

(defn param-contains 
  ([& expected-params]
    (fn [results]
      (every? true? (map #((contains %1) %2) expected-params results)))))

(facts "param-contains"
       ((param-contains {:a 1} {:b 2}) [{:a 1} {:b 2}])
       ((param-contains {:a 1} {:b 2}) [{:a 1 :x 1000} {:b 2}])
       ((param-contains {:a 1 :x 1000} {:b 2}) [{:a 1} {:b 2}]) => falsey
       ((param-contains {:b 2} {:a 1}) [{:a 1} {:b 2}]) => falsey
       )

(facts "simplification"
       (parsing/simplify {:type :whatever} 2) => (throws IllegalArgumentException #"whatever.*unknown"))

(facts "single field"
       (parsing/parse :field "speed     [0 100]") => (contains {:speed (contains {:type :range})}) 
       (parsing/parse :field "speed     accelerate \"quickly\" [0 100]") 
       => (contains 
            {:speed (contains {:type :function 
                               :name "accelerate"
                               :params (param-contains {:type :choice 
                                                        :item "quickly"} 
                                                       {:type :range})})})
       (parsing/parse :field "speed     accelerate (quickly [0 100])") 
       => (contains 
            {:speed (contains {:type :function 
                               :name "accelerate"
                               :params (param-contains {:type :function
                                                        :name "quickly"
                                                        :params (param-contains {:type :range})})})}))

(facts "multiple fields"
       (parsing/parse :fields "speed [0 100]") => (contains {:speed (contains {:type :range})}) 
       (parsing/parse :fields 
"speed     [0 100]
direction   <N NW W SW S SE E NE>
") => (contains {:speed (contains {:type :range}) 
                 :direction (contains {:type :list})}) 
       (parsing/parse :fields 
"speed     [0 100] # some comment
direction   <N NW W SW S SE E NE>") => (contains {:speed (contains {:type :range})
                 :direction (contains {:type :list})}) 

       (parsing/parse :fields"
info      format \"Speed %s km/h heading %s\" $speed $heading # with comment

   # these are just supporting fields, used for the calculation of the info-field
   speed     [0 100] # with comment
   heading   [0 360]") => (contains {:speed (contains {:type :range})
                 :heading (contains {:type :range})
                 :info (contains {:type :function
                                  :name "format"
                                  :params (param-contains {:type :choice}
                                                          {:type :field
                                                           :field :speed}
                                                          {:type :field
                                                           :field :heading})})})
)

