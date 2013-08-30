(ns fabulate.scratch
    (:use fabulate.core)
    (:require [fabulate.parsing :as parsing]))

(defn flatten-tree [wt] (if (empty? wt) nil (conj 
                                              (concat (flatten-tree (:less wt)) 
                                                      (flatten-tree (:more wt)))
                                              (:item wt))))
(defmulti depends-on (fn [field] (:type field)))

(defn dependencies [l]
		(->> l
    (map depends-on)
    (apply clojure.set/union))) 

(defmethod depends-on :choice [field] #{})
(defmethod depends-on :range [field] #{})
(defmethod depends-on :field [field] #{(:field field)})
(defmethod depends-on :list [field] (dependencies (flatten-tree (:wtree field))))
(defmethod depends-on :function [field] (dependencies (:params field)))
(comment

  (def fields (fabulate.parsing/parse :fields "speed     [0 100]
heading   [0 360]
info      format \"Speed %s km/h heading %s\" $speed $heading"))
  (into  {} (map (fn [kw] {kw (depends-on (kw fs))}) (keys fs)))

(binding [*row* (into (resolve fs :speed) (resolve fs :heading))
          *schema* fs]
  (into *row* (resolve fs :info)))

(keys fs)
; how to pass ctx (current row) to choose, as well as current rngs?
; dynamic vars? (def :^dynamic *ctx* {})
; (choose lookup in *ctx*)

(reduce (fn [row f] (into row (binding [*row* row] (resolve fs f)))) {} (keys fs))

  )
