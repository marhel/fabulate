(ns fabulate.range)
(set! *warn-on-reflection* true)

(defn- find-zero [a b c]
  ; the zeroes are x = (-b ± sqrt(d)) / 2a
  ; where d  = b^2 - 4ac
  (let [;_ (prn a b c)
        d (- (Math/pow b 2) (* 4 a c))
        root (fn [op] (/ (op (- b) (Math/sqrt d)) (* 2 a)))]
    (if (zero? a) 
      (- (* b c))
      ;we only need the first zero of [(root +) (root -)]
      (root +))))

(defn area [c1 c2]
  ; If the range is plotted with the intervals on X axis and the weights on the Y axis  
  ; the lookup is done by area under the line.
  ; The area under the linear function kx + c in interval [0 r] 
  ; is A = (k/2)r^2 + cr
  (let [r (- (:item c2) (:item c1))]
    (let [w-diff (- (:weight c2) (:weight c1))
          k (if (zero? r) 0 (/ w-diff r))
          c (:weight c1)
          a (/ k 2)
          A (+ (* a (Math/pow r 2)) (* c r))]
      [A a c r])))

(defn range-lookup
  "Works on simplified ranges (exactly two elements)"
  [range v]
  ; If the range is plotted with the intervals on X axis and the weights on the Y axis  
  ; the lookup is done by area under the line. A lookup of 0.5 finds the x-value where
  ; the area is 0.5 times the maximum area.
  ; The area under the linear function kx + c in interval [0 r] 
  ; is A = (k/2)r^2 + cr
  ; This formula has the shape of a quadratic function ax^2 + bx + c = 0
  ; or (k/2)r^2 + cr - A = 0
  ; Thus we can find the point where the area is A by finding the zeroes of this function
  ; Lookup of 0.5 in range [100:10 112:15] would be shifted to [0:10 12:15]
  ; r = 12, k = 5/12, c = 10 gives an area of 150. A lookup of 0.5 finds the x-value where the area is halved
  ; which is at the zeroes of 5/12/2*x^2 + 10*x - 75 of which one is found at ≈6.59
  ; which is then shifted back to the [100 112] range, resulting in 106.59 
  ; (instead of 106 in the middle of the range, had the range endpoints been equally weighted)
  (let [items (:items range)
        [c1 c2] (first (partition 2 1 items))
        [A a b r] (area c1 c2)
        ; _ (prn A a b r)
        ]
    (+ (:item c1) 
       (if (zero? a)
         (* v r)
         (find-zero a b (- (* A v)))))))
