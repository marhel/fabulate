(ns fabulate.main
  (:use fabulate.core)
  (:require [fabulate.parsing :as parsing]))

(defn -main [& args]
  (let [n 10
        code (format "{%s}" (clojure.string/join " " args))
        pt (parsing/parse :choice code)
        rnds (make-rand-seq (System/currentTimeMillis))] 
      (println "Fabulate 0.0.1α - Martin Hellspong")
      (println code)
      (println pt)
      (dotimes [i n]
        (println (choose pt (first (rnds 1)))))))
