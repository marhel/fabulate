(ns fabulate.scratch
  (:import (java.net InetAddress))
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
  (let [args "--count 1 -s speed,info csv --separator=*"
        argv (.split args " ")
        opts (fabulate.main/options-from-arguments argv)]
    (write-to opts fields)
    )

  (reduce (fn [row f] (into row (binding [*row* row] (resolve fs f)))) {} (keys fs)))

;;; move into fabulate.writer.csv namespace
(require '[clojure.data.csv :as csv])
(require '[clojure.java.io :as io])

(keys (methods write-to))

(def fields (fabulate.parsing/parse :fields
                                    "speed     [0 100]
                                          info      format \"Speed %s km/h heading %s\" $speed $heading
                                          heading   [0 360]
"))
;(def sf (reduce #(assoc-in %1 [%2 :selected] true) fields selection))
;(map name (filter #(get-in sf [% :selected]) (keys sf)))

(slurp "test/output/out.csv")