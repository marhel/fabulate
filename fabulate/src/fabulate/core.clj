(ns fabulate.core
  (:use fabulate.range))

(set! *warn-on-reflection* true)

(defn weighted-tree 
  ([xs]
    (cond 
      (map? xs) (weighted-tree xs val key)
      :else (weighted-tree xs (constantly 1))))
  ([xs weight]
    (weighted-tree xs weight identity))
  
  ([xs weight key]
    (let [descendingWeight (comp - weight)
          has-weight (comp pos? weight)
          by-weight (vec (sort-by descendingWeight (filter has-weight xs)))
          len (count by-weight)]
      (defn weight-ranges
        [pos start]
        (let [item       (nth by-weight pos)
              own-weight (weight item)
              pos-less   (inc (* 2 pos))
              ltree      (if (< pos-less len) (weight-ranges pos-less start))
              lsum       (:sum ltree 0)
              end        (+ start own-weight lsum)
              pos-more   (inc pos-less)
              mtree      (if (< pos-more len) (weight-ranges pos-more end))
              msum       (:sum mtree 0)]
          
          {:sum (+ own-weight lsum msum)
           :weight own-weight
           :start (+ start lsum)
           :end end
           :item (key item)
           :less ltree 
           :more mtree
           })))
    (weight-ranges 0 0)))

(defn lookup 
  "looks up value in weighted tree, returns item"
  ([wt value] 
    ;(println (get-in wt [:item :name]) (:start wt) (:end wt))
    (cond
      (< value (:start wt)) (if-let [less (:less wt)] 
                              (recur less value)
                              (throw (IllegalArgumentException. 
                                       (format "Cannot search for %s. Index cannot be lower than 0." value))))
      (>= value (:end wt))  (if-let [more (:more wt)]
                              (recur more value)
                              (throw (IllegalArgumentException. 
                                       (format "Cannot search for %s. Index cannot be %s or higher." value (:end wt)))))
      :else [(:item wt) (/ (- value (:start wt)) (:weight wt))])))

(defn make-rand-seq [seed]
  "Infinite random number sequence maker, with an initial seed for repeatable sequences"
  (let [r (java.util.Random. seed)]
    (fn nextNumber [n] (lazy-seq 
                         (cons (* n (.nextDouble r)) (nextNumber n))))))

(defmulti choose (fn [tree r] 
                   (do 
                     ;(prn (:type tree) r) 
                     (:type tree))))

(defmethod choose :choice [tree r]
   (:item tree))

(defmethod choose :list [tree r]
  (let [R (* r (:sum tree))
        [item hit] (lookup (:wtree tree) R)
        ; _ (prn item hit)
        ]
    (choose item hit)))

(defmethod choose :range [tree r]
  (range-lookup tree r))

; A function will need to consume as many random numbers as it has parameters, 
; as it will call choose on each argument
(defmethod choose :function [tree r]  
  (let [params (:params tree)
        vals (map #(choose % r) params)] 
    ; (println "calling" (:fn tree) vals)
    (apply (:fn tree) vals)))


(defn flatten-tree [wt] (if (empty? wt) nil (conj 
                                              (concat (flatten-tree (:less wt)) 
                                                      (flatten-tree (:more wt)))
                                              (:item wt))))
(defn dependencies [l]
		(->> l
    (map depends-on)
    (apply clojure.set/union))) 

(defmulti depends-on (fn [field] (:type field)))
(defmethod depends-on :choice [field] #{})
(defmethod depends-on :range [field] #{})
(defmethod depends-on :field [field] #{(:field field)})
(defmethod depends-on :list [field] (dependencies (flatten-tree (:wtree field))))
(defmethod depends-on :function [field] (dependencies (:params field)))

(defn generate 
  [fields f]
  (depends-on (f fields)))
;(into  {} (map (fn [kw] {kw (depends-on (kw fs))}) (keys fs)))