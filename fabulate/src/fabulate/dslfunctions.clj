(ns fabulate.dslfunctions)

(defn age [f] 
  (->> 
    (* f 365.25 24 60 60 1000)
    (- (java.lang.System/currentTimeMillis))
    long
    (java.util.Date.)
    (.format (java.text.SimpleDateFormat. "yyyy-MM-dd"))))

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
