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


(require '[clojure.data.csv :as csv])
(require '[clojure.java.io :as io])
(defn write-to [file-name fs fields records]
  (let [headers (map name fields)
        to-values (apply clojure.core/juxt fields)
        stream-of (fn further [fs]
                    (cons (to-values (generate fs)) (lazy-seq (further fs))))]
    (with-open [out-file (io/writer file-name)]
      (csv/write-csv out-file
                     (lazy-cat [headers]
                               (take records (stream-of fs)))))))

(def fields [:Type :id :Name :price])
(def fs (fabulate.parsing/parse :fields 
                                "Name    /\\w{3,9}/
   id /[A-F0-9]{8}(-[A-F0-9]{4}){3}-[A-F0-9]{12}/
   Type {one \"the second\" three}
   price price [0 100]"))

(write-to "test/output/out-file.csv" fs fields 100)
(slurp "test/output/out-file.csv")

