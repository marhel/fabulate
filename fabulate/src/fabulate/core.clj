(ns fabulate.core
  (:import (java.util Random))
  (:use fabulate.range)
  (:require [fabulate.kahn :as kahn]))

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
  (let [r (Random. seed)]
    (fn nextNumber [n] (lazy-seq 
                         (cons (* n (.nextDouble r)) (nextNumber n))))))

(def ^:dynamic *rnd* (make-rand-seq (System/currentTimeMillis)))

(defmulti choose (fn [field r]
                   ;(prn (:type field) r)
                   (:type field)))

(defmethod choose :choice [field r]
   (:item field))

(defmethod choose :list [field r]
  (let [R (* r (:sum field))
        [item hit] (lookup (:wtree field) R)
        ; _ (prn item hit)
        ]
    (choose item hit)))

(defmethod choose :range [field r]
  (range-lookup field r))

; A function will need to consume as many random numbers as it has parameters, 
; as it will call choose on each argument, however, it makes a private rand-seq for this purpose.
; This allows me to keep the old interface with a single random float.
; In order to try to keep the repeatability for randomness, it uses the random it got
; as a seed for the new rand-seq.
(defmethod choose :function [field r]
  (let [params (:params field)
        param-rand (make-rand-seq (* Long/MAX_VALUE r))
        vals (map choose params (param-rand 1))]
    ; (println "calling" (:fn field) vals)
    (apply (:fn field) vals)))

(def ^:dynamic *row* {})

(declare name-to-ctx)
(defn field-to-xref [field]
  (let [ctx (:xref field)]
    (if (nil? ctx)
      (throw (IllegalArgumentException. (format "field reference '%s' hasn't been fully resolved (no :xref)" (:name field))))
      ctx)))

; cross-reference to the value of another field in the same row
(defmethod choose :fieldref [field r]
  (let [ctx (field-to-xref field)]
    (get-in *row* ctx)))

(defmethod choose :regex [field r]
  (let [generator (:generator field)
        result (generator)
        extractor (if (vector? result) first identity)]
    (extractor result)))

(defn field-from-ctx [fields ctx]
  (get-in fields (interpose :fields ctx)))

(defn resolve-field
  [fields ctx]
  (let [field (field-from-ctx fields ctx)]
    (choose field (first (*rnd* 1)))))

(declare generate)
(defmethod choose :prototype [proto r]
  :nothing)

(defn flatten-tree [wt] (when (seq wt)
                          (conj
                            (concat (flatten-tree (:less wt))
                                    (flatten-tree (:more wt)))
                            (:item wt))))

(defn same-prefix [px l2]
  (if (or (empty? px) (empty? l2))
    (empty? px)
    (and (= (first px) (first l2)) (recur (rest px) (rest l2)))))
(declare field-ctx)
(defn field-ctxs [fields]
  (mapcat field-ctx fields))
(defn- field-ctx [[f def]]
  (if (= :prototype (:type def))
    (field-ctxs (:fields def))
    [(:ctx def)]
    ))
(defn ctx-to-name [ctx]
  (clojure.string/join "." (map name ctx)))
(defn name-to-ctx [fname]
  (vec (map keyword (clojure.string/split fname #"\."))))
(defn copy-by-ctx [m ctxs]
  (reduce (fn [row ctx] (assoc-in row ctx (get-in m ctx)))
          {} (reverse ctxs)))                               ; reverse seems to help get the fields output in the proper order when mapping over the generated hashmap
(defn lookup-field [fname fields]
  (let [path (reverse (name-to-ctx fname))
        cxr (map reverse (field-ctxs fields))
        [first & rest :as all] (map (comp vec reverse) (filter #(same-prefix path %) cxr))]
    (cond (empty? first) (throw (IllegalArgumentException. (format "field reference '%s' matches no known field" fname)))
          (empty? rest)  first
          :else          (throw (IllegalArgumentException. (format "field reference '%s' matches more than one field; '%s'" fname (clojure.string/join "', '" (map ctx-to-name all))))))))

(defmulti depends-on (fn [field fields] (:type field)))

(defn dependencies [l fields]
  (->> l
       (map #(depends-on % fields))
       (apply clojure.set/union)))

(defmethod depends-on :default [field fields] #{})
(defmethod depends-on :fieldref [field fields] #{(field-to-xref field)})
(defmethod depends-on :list [field fields] (dependencies (flatten-tree (:wtree field)) fields))
(defmethod depends-on :function [field fields] (dependencies (:params field) fields))

(defn fields-by-dep
  ([fields selection]
   (let [selected? (set selection)
         dependency (fn [ctx] {ctx (depends-on (field-from-ctx fields ctx) fields)})
         ctxs (->> (filter selected? (field-ctxs fields))
                   (map dependency)
                   (into {})
                   (kahn/kahn-sort)
                   (reverse))
         no-new-deps? (= (set ctxs) selected?)]
     (if no-new-deps?
       ctxs
       ; ensure that the dependencies of any new dependencies are also included
       (recur fields ctxs))))
  ([fields]
   (fields-by-dep fields (field-ctxs fields))))

(defn generate
  ([fields]
   (generate fields (fields-by-dep fields)))
  ([fields ctxs-in-dep-order]
   (reduce (fn [row ctx]
             (binding [*row* row]
               (assoc-in row ctx (resolve-field fields ctx)))) {} ctxs-in-dep-order)))


(defmulti write-to (fn [opts fields]
                     (:writer opts)))

(defn subcommand [argv] (keyword (first argv)))
(defmulti parse-subcommand subcommand)

(defmethod parse-subcommand :default [argv] {})
