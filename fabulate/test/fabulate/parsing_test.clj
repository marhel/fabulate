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

(defn parse-choice [dsl] (parsing/parse dsl :choice))
(defn parse-field [dsl] (parsing/parse dsl :field))

(facts "single choice"
       (parse-choice "abc") => {:type :choice :weight 1 :item "abc"}
       (parse-choice "123") => {:type :choice :weight 1 :item 123}
       (parse-choice "123.456") => {:type :choice :weight 1 :item 123.456}
       )

(facts "list of choices"
       (parse-choice "{red green blue}") => (contains {:type :list :wtree (count-is 3)})
       (parse-choice "{red green blue}") => (contains {:type :list :wtree (tree-contains ["red" "green" "blue"])})
       (parse-choice "{red \"green blue\"}") => (contains {:type :list :wtree (tree-contains ["red" "green blue"])})
       (parse-choice "{1 2 3}") => (contains {:type :list :wtree (count-is 3)})
       (parse-choice "{1 2 3}") => (contains {:wtree (tree-contains [1 2 3])})
       (parse-choice "{1 2 3}") => (contains {:wtree (tree-contains [1 1 1] :weight)})
)

(facts "list of weighted choices"
       (parse-choice "{red green:70 blue}") => (contains {:wtree (tree-contains [1 1 70] :weight)})
       (parse-choice "{red:10 green:70 blue:20}") => (contains {:type :list :wtree (tree-contains ["red" "green" "blue"])})
       (parse-choice "{red:10 green:70 blue:20}") => (contains {:type :list :wtree (tree-contains [10 20 70] :weight)})
       (parse-choice "{1:20 2:30 3:40}") => (contains {:type :list :wtree (count-is 3)})
       (parse-choice "{1:20 2:30 3:40}") => (contains {:type :list :wtree (tree-contains [30 20 40] :weight)})
)
(defn range-contains 
  ([expected-elements]
    (range-contains expected-elements :item))
  ([expected-elements f] 
    (fn [r]
      ((just expected-elements :in-any-order) (map f r)))))

(facts "a range"
       (parse-choice "[1 20]") => (contains {:type :range :items (range-contains [1 20])})
       (parse-choice "[1 20]") => (contains {:type :range :items (range-contains [1 1] :weight)})
       (parse-choice "[1:100 20:50]") => (contains {:type :range :items (range-contains [1 20])})
       (parse-choice "[1:100 20:50]") => (contains {:type :range :items (range-contains [100 50] :weight)})
)

(facts "errors"
       (parse-choice "abc(def") => insta/failure?
       (parse-choice "abc def") => insta/failure?
       (parse-choice "1 2") => insta/failure?
       (parse-choice "") => insta/failure?
       )

(facts "non-ASCII character handling"
       (parse-choice "{abc åÄÖ 123}") => (contains {:type :list :wtree (tree-contains ["abc" "åÄÖ" 123])})
       (parse-choice "{üb.er lambda_λ}") => (contains {:type :list :wtree (tree-contains ["üb.er" "lambda_λ"])})
       )

(facts "numbers"
       (parse-choice "123") => (contains {:item 123})
       (parse-choice "a123") => (contains {:item "a123"})
       (parse-choice "-123") => (contains {:item -123})
       (parse-choice "12a3bc") => (throws NumberFormatException #"12a3bc")
       (parse-choice "{123 \"abc\"}") => (contains {:wtree (tree-contains [123 "abc"])})
       (parse-choice "a123.456") => (contains {:item "a123.456"})
       (parse-choice "123.456") => (contains {:item 123.456})
       (parse-choice "-123.456") => (contains {:item -123.456})
       (parse-choice "-12a3.456x") => (throws NumberFormatException #"-12a3.456x")
       (parse-choice "1.23E05") => (contains {:item 123000.0})
       )

(facts "comments"
       (parse-choice "123 #abc") => (contains {:item 123})
       (parse-choice "123 # abc") => (contains {:item 123})
       (parse-choice "{123 \"# abc\" 456}") => (contains {:wtree (tree-contains [123 "# abc" 456])})
       (parse-choice "{123 # abc\n456}") => (contains {:wtree (tree-contains [123 456])})
       (parse-choice "{123 # abc\r456}") => (contains {:wtree (tree-contains [123 456])})
       )