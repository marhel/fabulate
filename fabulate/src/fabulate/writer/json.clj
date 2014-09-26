(ns fabulate.writer.json
  (:use fabulate.core)
  (:require [clojure.java.io :as io])
  (:require [clojure.data.json :as json])
  (:require [clojure.tools.cli :as cli]))

(defmethod parse-subcommand :json [argv]
  (let [options  [["-d" "--destination FILE" "Destination file"]
                  ]]
    (cli/parse-opts (rest argv) options)))

(defmethod write-to :json [opts fields]
  (let [headers   (:select opts)
        selection (map keyword headers)
        file-name (:destination opts)
        num-recs  (:count opts)
        subset-by-dep (fields-by-dep fields selection)
        stream-of (fn further [fields]
                    (cons (select-keys (generate fields subset-by-dep) selection) (lazy-seq (further fields))))
        stream-n  (take num-recs (stream-of fields))
        write     #(json/pprint stream-n)]
    (if file-name
      (with-open [out-file (io/writer file-name)]
        (binding [*out* out-file]
          (write)))
      (write))))
