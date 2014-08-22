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

(facts "round-sig"
       (dsl/round-sig 0 4) => 0
       (dsl/round-sig 1.23456789e15 4) => 1.235e15
       (dsl/round-sig 1.23456789e14 4) => 1.235e14
       (dsl/round-sig 1.23456789e13 4) => 1.235e13
       (dsl/round-sig 1.23456789e12 4) => 1.235e12
       (dsl/round-sig 1.23456789e11 4) => 1.235e11
       (dsl/round-sig 1.23456789e10 4) => 1.235e10
       (dsl/round-sig 1.23456789e09 4) => 1.235e9
       (dsl/round-sig 1.23456789e08 4) => 1.235e8
       (dsl/round-sig 1.23456789e07 4) => 1.235e7
       (dsl/round-sig 1.23456789e06 4) => 1.235e6
       (dsl/round-sig 1.23456789e05 4) => 1.235e5
       (dsl/round-sig 1.23456789e04 4) => 1.235e4
       (dsl/round-sig 1.23456789e03 4) => 1.235e3
       (dsl/round-sig 1.23456789e02 4) => 1.235e2
       (dsl/round-sig 1.23456789e01 4) => 1.235e1
       (dsl/round-sig 1.23456789e00 4) => 1.235e0
       (dsl/round-sig 1.23456789e-1 4) => 1.235e-1
       (dsl/round-sig 1.23456789e-2 4) => 1.235e-2
       (dsl/round-sig 1.23456789e-3 4) => 1.235e-3
       (dsl/round-sig 1.23456789e-4 4) => 1.235e-4
       (dsl/round-sig 1.23456789e-5 4) => 1.235e-5
       (dsl/round-sig 1.23456789e-6 4) => 1.235e-6
       (dsl/round-sig 1.23456789e-7 4) => 1.235e-7
       (dsl/round-sig 1.23456789e-8 4) => 1.235e-8
       (dsl/round-sig 1.23456789e-9 4) => 1.235e-9
       )

(facts "price"
       (dsl/price 0) => 0
       (dsl/price -10) => -10.0
       (dsl/price 1.23456789e13) => 1.235e13
       (dsl/price 1.23456789e07) => 1.235e7
       (dsl/price 1.23456789e05) => 1.235e5
       (dsl/price 1.23456789e03) => 1.235e3
       (dsl/price 1.23456789e02) => 123.5
       (dsl/price 1.23456789e01) => 12.3
       (dsl/price 1.23456789e00) => 1.2
       (dsl/price 1.23456789e-1) => 0.1
       (dsl/price 1.23456789e-2) => 0.0
       (dsl/price 1.23456789e-4) => 0.0
       (dsl/price 1.23456789e-8) => 0.0
       )

#_(facts "main"
       (let [code "speed     [0 100]
heading   [0 360]
info      format \"Speed %.2f km/h heading %.2f\" $speed $heading
"
             file "test/output/fabtest.fab"
             _ (spit file code)] 
         (main/-main file) => anything
         (main/-main file "2") => anything 
       ))