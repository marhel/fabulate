(ns fabulate.core-test
  (:use midje.sweet)
  (:require [fabulate.core :as core]
            [fabulate.parsing :as parsing]
            [fabulate.kahn :as kahn]))

(def colors {"Red" 10 "Green" 70 "Blue" 20})
(def fruits ["Apple" "Orange" "Pear" "Banana"])

(facts
  "weighted-tree"
  (core/weighted-tree colors) => (contains {:item "Green" :sum 100 :weight 70})
  (core/weighted-tree colors) => (contains {:item "Green" :sum 100 :weight 70})
  (core/weighted-tree colors) => (contains {:less (contains {:item "Blue" :sum 20 :weight 20})})
  (core/weighted-tree colors) => (contains {:more (contains {:item "Red" :sum 10 :weight 10})})
  (core/weighted-tree fruits) => (contains {:sum 4})
)

(facts 
  "tree lookup"
  (let [color (core/weighted-tree colors)] 
    (core/lookup color 0) => ["Blue" 0] ; [0-20)
    (core/lookup color 10) => ["Blue" 1/2]
    (core/lookup color 20) => ["Green" 0] ; [20-90)         
    (core/lookup color 55) => ["Green" 1/2]
    (core/lookup color 89.999) => (contains "Green" (roughly 0.99999))
    (core/lookup color 90) => ["Red" 0] ; [90-100)
    (core/lookup color 99.9999) => (contains "Red" (roughly 0.99999))
    ; out-of-bounds values throws
    (core/lookup color 100) => (throws IllegalArgumentException #"\b100\b")
    (core/lookup color 234) => (throws IllegalArgumentException #"\b234\b" #"\b100\b")
    (core/lookup color -123) => (throws IllegalArgumentException #"-123\b" #"\b0\b")))

(def first3 [41.321922662961654 99.0414373129817 25.225544079393046])
(def well-known-seed 123456)

(facts 
  "rand-seq is predictable" 
  (let [rand-seq (core/make-rand-seq well-known-seed)]
    rand-seq => fn?
    ; the rand-seq produces a known ("infinite") sequence from this seed
    (take 3 (rand-seq 100)) => (just first3)
    ; as well as not restarting when called again
    (take 3 (rand-seq 100)) => (just [70.56636259919212 7.343842024000635 53.60645079196139])
    (take 3 (rand-seq 100)) => (just [41.019319216746965 52.31526988972157 89.67743965315603]) 
    (take 3 (rand-seq 100)) => (just [67.74060392059476 44.46627630598815 11.219855161526382]))
  ; but a re-start of the sequence can be done by re-creating the sequence with the same seed
  (let [restarted-seq (core/make-rand-seq well-known-seed)] 
    (take 3 (restarted-seq 100)) => (just first3)))

(facts
  "lookup from rand-seq is predictable"
  (let [rand-seq (core/make-rand-seq well-known-seed)
        color (core/weighted-tree colors)]
    (->> (rand-seq 100)
      (map #(core/lookup color %))
      (map first)
      (take 12)) => (just ["Green" "Red" "Green" 
                           "Green" "Blue" "Green"
                           "Green" "Green" "Green"
                           "Green" "Green" "Blue"])))
(defn matches
  [pattern]
    (fn [actual] (not (nil? (re-matches pattern actual)))))

(facts "depends-on"
       (core/depends-on (:zero (parsing/parse :field "zero <$one two $three>"))) => (just #{:one :three})
       (core/depends-on (:zero (parsing/parse :field "zero /123/"))) => (just #{}))

(facts "choose"
       (core/choose (parsing/parse :choice "[0 100]") 0.5) => (roughly 50)
       (core/choose (parsing/parse :choice "[0:0 100:0]") 0.5) => (roughly 50) ; iffy, should throw!
       (core/choose (parsing/parse :choice "<0 100>") 0.1) => (some-checker 100 0)
       (core/choose (parsing/parse :choice "100") 0.1) => 100
       (core/choose (parsing/parse :choice "(int [0 100])") 0.5) => 73
       (core/choose (parsing/parse :choice "100") 0.1) => 100
       (core/choose (parsing/parse :choice "/<[A-Z]>/") 0.1) => (matches #"<[A-Z]>")
       (core/choose (parsing/parse :choice "/([A-Z])/") 0.1) => (matches #"[A-Z]")
       )

(facts "kahn"
       (let [acyclic-g {:A1 #{:A2 :B2 :C2}
                        :B1 #{:A2 :B2 :C2}
                        :C1 #{:A2 :B2 :C2}
                        :A2 #{:A3 :B3 :C3}
                        :B2 #{:A3 :B3 :C3}
                        :C2 #{:A3 :B3 :C3}}
             [p1 p2 p3] (partition 3 (kahn/kahn-sort acyclic-g))] 
         p1 => (just [:A1 :B1 :C1] :in-any-order)
         p2 => (just [:A2 :B2 :C2] :in-any-order)
         p3 => (just [:A3 :B3 :C3] :in-any-order)
         )
       (let [cyclic-g  {7 #{11 8}
                        5 #{11}
                        3 #{8 10}
                        11 #{2 9}
                        8 #{9}
                        2 #{11}}] ; oops, a cycle!
         (kahn/kahn-sort cyclic-g) => nil))

(facts
  "field dependencies can be deduced, even from a subset of fields"
  (let [dsl "speed     [0 100]
info      format \"Speed %.2f km/h Heading %.2f\" $speed $heading
heading   [0 360]
distance  [10 40]
extended  format \"%s Distance %.2f\" $info $distance
"
        fields (parsing/parse :fields dsl)]
    (core/fields-by-dep fields) => (just [:heading :distance :speed :info :extended])
    (core/fields-by-dep fields [:info]) => (just [:heading :speed :info]) ; includes dependencies of the specified subset as well
    (core/fields-by-dep fields [:extended]) => (just [:heading :distance :speed :info :extended]) ; includes dependencies recursively
    (core/fields-by-dep fields [:info :heading :speed]) => (just [:heading :speed :info])
    (core/fields-by-dep fields [:speed]) => (just [:speed])))

(facts
  "generate row generates only requested fields"
  (let [dsl "speed     [0 100]
info      format \"Speed %.2f km/h heading %.2f\" $speed $heading
heading   [0 360]
distance  [10 40]
"
        fields (parsing/parse :fields dsl)]
    (binding [core/*rnd*  (core/make-rand-seq well-known-seed)]
      (core/generate fields) => {:distance 39.71243119389451 :speed 25.225544079393046, :heading 148.75892158666196, :info "Speed 25,23 km/h heading 148,76"}
      (core/generate fields) => {:distance 26.081935237588418 :speed 41.019319216746965 :heading 26.437831286402286 :info "Speed 41,02 km/h heading 26,44"}
      (core/generate fields [:speed]) => {:speed 89.67743965315603}
      (core/generate fields [:info]) => {:info "Speed nu km/h heading nu"} ; generate won't automatically include dependent fields (which resolves to nil (or "nu"(ll) in the string formatting)
      (core/generate fields [:heading :speed :info]) => {:heading 160.07859470155734, :info "Speed 11,22 km/h heading 160,08", :speed 11.219855161526382}
      )))

(facts
  "fun with many params picks different param for each"
  (let [dsl "nums	format \"%s %s %s %s %s %s %s %s %s %s\" <1 2 3 4 5 6 7 8 9 10> <1 2 3 4 5 6 7 8 9 10> <1 2 3 4 5 6 7 8 9 10> <1 2 3 4 5 6 7 8 9 10> <1 2 3 4 5 6 7 8 9 10> <1 2 3 4 5 6 7 8 9 10> <1 2 3 4 5 6 7 8 9 10> <1 2 3 4 5 6 7 8 9 10> <1 2 3 4 5 6 7 8 9 10> <1 2 3 4 5 6 7 8 9 10>
"
        fields (parsing/parse :fields dsl)]
    (binding [core/*rnd*  (core/make-rand-seq well-known-seed)]
      (let [ns (:nums (core/generate fields))]
        (count
          (distinct (.split ns " "))) =not=> (throws Exception)
        (count
          (distinct (.split ns " "))) =not=> 1                  ; because we use our well-known-seed, shouldn't be just a repetition
        ))))
