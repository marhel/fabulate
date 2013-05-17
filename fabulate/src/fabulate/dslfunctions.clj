(ns fabulate.dslfunctions)

(defn age [f] 
  (->> 
    (* f 365.25 24 60 60 1000)
    (- (java.lang.System/currentTimeMillis))
    long
    (java.util.Date.)
    (.format (java.text.SimpleDateFormat. "yyyy-MM-dd"))))
