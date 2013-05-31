(ns fabulate.dslfunctions-test
  (:use midje.sweet)
  (:require [fabulate.dslfunctions :as dsl]
            [fabulate.main :as main]))
  
(defn now [] (java.util.Date.))
(defn date-like [fmt]
  (fn [actual]
    ((has-prefix (.format (java.text.SimpleDateFormat. fmt) (now))) actual)))

(facts "age"
       (dsl/age 0) => (date-like "yyyy"))

(facts "main"
       (main/-main "(age [20 30])") => anything)