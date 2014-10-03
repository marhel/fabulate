(ns fabulate.writer.json
  (:use fabulate.core)
  (:require [clojure.java.io :as io])
  (:require [clojure.data.json :as json])
  (:require [clojure.tools.cli :as cli]))

(defmethod write-to :json [writer opts fields stream-n]
  (binding [*out* writer]
    (json/pprint stream-n)))
