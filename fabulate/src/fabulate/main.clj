(ns fabulate.main
  (:use fabulate.core)
  (:require [fabulate.parsing :as parsing]))

(defn -main [& args]
  (let [file (first args)
        n (read-string (or (second args) "10"))
        code (slurp file)
        fields (parsing/parse :fields code)]
      (println "Fabulate 0.0.2α - Martin Hellspong")
      (println "-- Code from" file "--\n\n" code "\n\n-- Fields --\n" fields "\n\n--" n "Results --")
      (dotimes [i n]
        (prn (generate fields)))))
