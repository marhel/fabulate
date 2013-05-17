(ns fabulate.range-test
  (:use midje.sweet)
  (:require [fabulate.range :as range]))

(defn a-range [f t]
  (let [[fv fw] f
        [tv tw] t]
    {:type :range, 
     :items [{:type :choice, :weight fw, :item fv} {:type :choice, :weight tw, :item tv}]}))

(defn cx-range [& fs]
  (let [items (map #(let [[fv fw] %]
                      {:type :choice, :weight fw, :item fv}) fs)]
    {:type :range, :items items}))

(defn midpoint-of 
  [f t]
  (range/range-lookup (a-range f t) 0.5))

(facts "flat-weighted ranges"
       (midpoint-of [0 1] [10 1]) => (roughly 5)
       ; weights should be irrelevant as long as they're the same
       (midpoint-of [0 100] [10 100]) => (roughly 5)
       (midpoint-of [100 1] [110 1]) => (roughly 105)
       (midpoint-of [100 100] [110 100]) => (roughly 105)
       )

(def off-center 7.071) ; had been 5 if the ranges were flat-weighted
(facts "sloping ranges"
       ; positive values, positive slope
       (midpoint-of [0 0] [10 10]) => (roughly off-center)
       ; swap weights, negative slope
       (midpoint-of [0 10] [10 0]) => (roughly (- 10 off-center))
       ; swap values, negative range with negative slope
       (midpoint-of [10 10] [0 0]) => (roughly off-center)
       ; swap weights, negative range
       (midpoint-of [10 0] [0 10]) => (roughly (- 10 off-center))
       ; translate +100
       (midpoint-of [100 0] [110 10]) => (roughly (+ 100 off-center))
       (midpoint-of [100 10] [110 0]) => (roughly (- 110 off-center))
       (midpoint-of [110 10] [100 0]) => (roughly (+ 100 off-center))
       (midpoint-of [110 0] [100 10]) => (roughly (- 110 off-center))
       ; negative values, positive slope
       (midpoint-of [-100 0] [-110 10]) => (roughly (- (+ 100 off-center)))
       (midpoint-of [-100 10] [-110 0]) => (roughly (- (- 110 off-center)))
       (midpoint-of [-110 10] [-100 0]) => (roughly (- (+ 100 off-center)))
       (midpoint-of [-110 0] [-100 10]) => (roughly (- (- 110 off-center))))

;(facts "negative weights"
;       (midpoint-of [10 10] [20 -5]) => midje.checkers/falsey
;       (midpoint-of [10 -10] [20 5]) => midje.checkers/falsey
;       (midpoint-of [10 -10] [20 -5]) => midje.checkers/falsey
;       )

(facts "zero weights"
       (midpoint-of [10 0] [20 0]) => (roughly 15)) ; iffy, should throw!

(facts "zero area"
       (#'fabulate.range/find-zero 0 2 3) => -6 
       (midpoint-of [10 0] [10 10]) => (roughly 10)) ; iffy, should throw!

(defn count-is [n]
  (fn [l]
    (= n (count l))))

(facts "complex ranges"
       (cx-range [10 1] [20 5] [30 1]) => (contains {:items (count-is 3)})
       ())