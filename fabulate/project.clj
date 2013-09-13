(defproject fabulate "0.1.0-SNAPSHOT"
  :description "Fabulate generates data from your specifications"
  :url "http://github.com/marhel/fabulate"
  :main fabulate.main
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [instaparse "1.2.2"]
                 [re-rand "0.1.0"]]
  :profiles {:dev {:dependencies [[midje "1.5.1"]]}})
