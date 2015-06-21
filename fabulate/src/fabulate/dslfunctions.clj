(ns fabulate.dslfunctions
  (:require [fabulate.core :refer [choose make-rand-seq]])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

(defn age [f] 
  (->> 
    (* f 365.25 24 60 60 1000)
    (- (System/currentTimeMillis))
    long
    (Date.)
    (.format (SimpleDateFormat. "yyyy-MM-dd"))))

(defn magnitude [n]
  (let [abs (if (neg? n) (- n) n)]
    (->> abs 
      Math/log10
      Math/floor
      int
      inc)))
          
(defn round-sig [num n]
  (if (zero? num) 
    0
    (let [m (magnitude num)
          mdiff (- n m)
          ; feeble attempt to avoid rounding errors in answer by working with large magnitudes instead of small
          [div mul mdiff] (if (neg? mdiff) [/ * (- mdiff)] [* / mdiff])
          mshift (Math/pow 10 mdiff)
          shifted (Math/round (div num mshift))]
      (mul shifted mshift))))

(defn price [f]
  (if (zero? f)
    0
    (let [m (magnitude f)
          s (inc (min 3 m))]
      (round-sig f s))))

; give legal names for these simple functions, as you aren't allowed to use a non-alfanumeric symbol name in the DSL at this point
(def add +)
(def subtract -)
(def multiply *)
(def divide /)

(defn repeat
  {:fabmacro true}
  [field r]
  (let [count-def (first (:params field))
        inner (last (:params field))
        count (int (choose count-def r))
        param-rand (make-rand-seq (* Long/MAX_VALUE r))
        result (take count (map choose (clojure.core/repeat count inner) (param-rand 1)))]
    result))