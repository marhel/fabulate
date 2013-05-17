(ns fabulate.main
  (:use fabulate.core)
  (:require [fabulate.parsing :as parsing]))

(defn -main [& args]
  (let [n 10
        code (format "{%s}" (clojure.string/join " " args))
        pt (parsing/parse code :choice)
        rnds (make-rand-seq (System/currentTimeMillis))] 
      (println "Fabulate 0.0.1α - Martin Hellspong")
      (println code)
      (println pt)
      (doseq [i (take n (rnds 1))]
        (println (choose pt i)))))
