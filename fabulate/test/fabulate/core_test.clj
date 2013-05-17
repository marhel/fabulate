(ns fabulate.core-test
  (:use midje.sweet)
  (:require [fabulate.core :as core]))

(def colors {"Red" 10 "Green" 70 "Blue" 20})
(def fruits ["Apple" "Orange" "Pear" "Banana"])

(defn x-contains [expected-elements]
  (fn [wt]
    (and
      (if-let [sum (:sum expected-elements)] 
        (= sum (.sum wt))
        true)
      (if-let [item (:item expected-elements)] 
        (= item (.item wt))
        true)
      (if-let [weight (:weight expected-elements)] 
        (= weight (.weight wt))
        true)
      (if-let [check (:less expected-elements)] 
        (check (.less wt))
        true)
      (if-let [check (:more expected-elements)] 
        (check (.more wt))
        true))))

(facts
  "weighted-tree"
  (core/weighted-tree colors) => (contains {:item "Green" :sum 100 :weight 70})
  (core/weighted-tree colors) => (contains {:item "Green" :sum 100 :weight 70})
  (core/weighted-tree colors) => (contains {:less (contains {:item "Blue" :sum 20 :weight 20})})
  (core/weighted-tree colors) => (contains {:more (contains {:item "Red" :sum 10 :weight 10})})
  (core/weighted-tree fruits) => (contains {:sum 4})
)

(defn any-of [expected-elements]
  (fn [actual]
    ((set expected-elements) actual)))

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
