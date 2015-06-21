(ns fabulate.dslmacros
  (:use fabulate.core))

(defn repeat [params inner r]
  (let [count-def (first params)
        count (int (choose count-def r))
        param-rand (make-rand-seq (* Long/MAX_VALUE r))
        result (take count (map choose (clojure.core/repeat count inner) (param-rand 1)))]
    result))

(defn sort [params inner r]
  (clojure.core/sort (choose inner r)))
