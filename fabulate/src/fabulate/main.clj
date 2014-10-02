(ns fabulate.main
  (:use fabulate.core)
  (:require [fabulate.parsing :as parsing])
  (:require [clojure.tools.cli :as cli])
  (:require [fabulate.writer.csv])
  (:require [fabulate.writer.json]))

(def cli-options
  [;; First three strings describe a short-option, long-option with optional
   ;; example argument description, and a description. All three are optional
   ;; and positional.
    ["-n" "--count ROWS" "Number of rows"
     :default 10
     :parse-fn #(Integer/parseInt %)
     :validate [#(< 0 % 1000000) "Must be a number between 0 and 1000000"]]
    ["-i" "--input FILE" "Input fabfile"]
    ["-s" "--select fields" "Comma separated list of fields to include in the output"
     :default-desc "All fields"
     :parse-fn #(vec (map (fn [s] (.trim s)) (.split % ",")))]
   ;; If no required argument description is given, the option is assumed to
   ;; be a boolean option defaulting to nil
    ["-h" "--help"]])

(defn options-from-arguments [argv]
  (let [main-cli  (cli/parse-opts argv cli-options :in-order true)
        writer    (subcommand (:arguments main-cli))
        main-cli  (assoc-in main-cli [:options :writer] writer)
        sub-cli   (parse-subcommand (:arguments main-cli))]
    (apply merge (map :options [main-cli sub-cli]))))

(defn -main [& args]
  (let [opts (options-from-arguments args)
        code (slurp (:input opts))
        prototype (parsing/parse :prototype code)
        fields (:fields ((first (keys prototype)) prototype))
        opts (if (:select opts) opts (assoc opts :select (map ctx-to-name (field-ctxs fields))))
        ]
    (binding [*out* *err*]
      (println "Fabulate 0.1α - Martin Hellspong"))
    (write-to opts fields)))