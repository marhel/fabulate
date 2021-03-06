(defproject fabulate "0.1.0-SNAPSHOT"
  :description "Fabulate generates data from your specifications"
  :url "http://github.com/marhel/fabulate"
  :main fabulate.main
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [instaparse "1.3.3"]
                 [re-rand "0.1.0"]
                 [org.clojure/data.csv "0.1.2"]
                 [org.clojure/data.json "0.2.5"]
                 [org.clojure/tools.cli "0.3.1"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]
                                  [org.clojure/tools.trace "0.7.8"]]}})
