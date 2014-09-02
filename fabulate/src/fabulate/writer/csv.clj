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
        selection (map keyword headers)
        file-name (:destination opts)
        num-recs  (:count opts)
        to-values (apply clojure.core/juxt selection)
        subset-by-dep (fields-by-dep fields selection)
        separator (or (:separator opts) \,)
        stream-of (fn further [fields]
                    (cons (to-values (generate fields subset-by-dep)) (lazy-seq (further fields))))
        stream-with-headers (lazy-cat [headers] (take num-recs (stream-of fields)))
        write     #(csv/write-csv % stream-with-headers :separator separator)]
    (if file-name
      (with-open [out-file (io/writer file-name)] (write out-file))
      (write *out*))))
