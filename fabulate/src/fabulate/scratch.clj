(ns fabulate.scratch
    (:use fabulate.core)
    (:require [fabulate.parsing :as parsing]))

(comment

  (def fields (fabulate.parsing/parse :fields "speed     [0 100]
info      format \"Speed %s km/h heading %s\" $speed $heading
heading   [0 360]
"))
  (->> (keys fields)
    (map (fn [kw] {kw (depends-on (kw fields))})) 
    (into  {})
    (fabulate.kahn/kahn-sort)
    (reverse))
  (fabulate.kahn/kahn-sort (into  {} (map (fn [kw] {kw (depends-on (kw fields))}) (keys fields))))


(binding [*row* (into (resolve fs :speed) (resolve fs :heading))
          *schema* fs]
  (into *row* (resolve fs :info)))

(keys fs)
; how to pass ctx (current row) to choose, as well as current rngs?
; dynamic vars? (def :^dynamic *ctx* {})
; (choose lookup in *ctx*)

(reduce (fn [row f] (into row (binding [*row* row] (resolve fs f)))) {} (keys fs)))
