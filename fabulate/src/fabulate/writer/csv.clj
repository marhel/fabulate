(ns fabulate.writer.csv
  (:use fabulate.core)
  (:require [clojure.java.io :as io])
  (:require [clojure.data.csv :as csv])
  (:require [clojure.tools.cli :as cli]))

(defmethod parse-subcommand :csv [argv]
  (let [options  [["-d" "--destination FILE" "Destination file"]
                  ["-s" "--separator CHAR" "Field separator to use" :parse-fn first]]]
    (cli/parse-opts (rest argv) options)))

(defmethod write-to :csv [opts fields]
  (let [headers   (:select opts)
        selection (map #(lookup-field % fields) headers)
        file-name (:destination opts)
        num-recs  (:count opts)
        subset-by-dep (fields-by-dep fields selection)
        separator (or (:separator opts) \,)
        stream-of (fn further [fields]
                    (cons (let [generated (generate fields subset-by-dep)]
                            (map #(get-in generated %) selection))
                          (lazy-seq (further fields))))
        stream-with-headers (lazy-cat [headers] (take num-recs (stream-of fields)))
        write     #(csv/write-csv % stream-with-headers :separator separator)]
    (if file-name
      (with-open [out-file (io/writer file-name)] (write out-file))
      (write *out*))))
