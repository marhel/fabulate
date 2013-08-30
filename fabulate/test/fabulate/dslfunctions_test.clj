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
       (let [code "speed     [0 100]
heading   [0 360]
info      format \"Speed %.2f km/h heading %.2f\" $speed $heading"
             file "fabtest.fab"
             _ (spit file code)] 
         (main/-main file) => anything))