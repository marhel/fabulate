(ns fabulate.writer.csv
  (:use fabulate.core)
  (:require [clojure.java.io :as io])
  (:require [clojure.data.csv :as csv])
  (:require [clojure.tools.cli :as cli]))

(defmethod parse-subcommand :csv [argv]
  (let [options  [["-s" "--separator CHAR" "Field separator to use" :parse-fn first]]]
    (cli/parse-opts (rest argv) options)))

(defmethod write-to :csv [writer opts fields stream-n]
  (let [headers (:select opts)
        selection (:selected-ctxs opts)
        separator (or (:separator opts) \,)
        selected-to-array (fn [generated] (map #(get-in generated %) selection))
        stream-with-headers (lazy-cat [headers] (map selected-to-array stream-n))]
    (csv/write-csv writer stream-with-headers :separator separator)))
