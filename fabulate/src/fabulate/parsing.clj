(ns fabulate.parsing
  (:use fabulate.core)
  (:use fabulate.range)
  (:require [fabulate.dslfunctions :as dsl])
  (:require [instaparse.core :as insta]))

(def choice-parser (insta/parser
    "fields = field+
     field = symbol <noise>? ( choice | function ) <noise>?
     choice = simple-choice <noise>? ( <':'> <noise>? number )?
     function = <noise>? symbol (<noise>? choice)*
     <simple-choice> = string | symbol | number | list | range | paren-function | fieldref
     string = <noise>? <quote> (!quote !escape #'.' | escaped-char)* <quote>
     symbol = !number word
     number = #'[-+]?[0-9][.\\w]*'
     list = <noise>? <'{'> (<noise>? choice)+ <noise>? <'}'>
     range = <noise>? <'['> (<noise>? &number choice)+ <noise>? <']'>
     <paren-function> = <noise>? <'('> function <noise>? <')'>
     fieldref = <'$'> symbol 
     <escaped-char> = escape any-or-newlines
     <quote> = '\\\"'
     <escape> = <'\\\\'>
     <any-or-newlines> = #'(.|\\n\\r?)'
     noise = (<comment> | <whitespace>)+
     comment = #'#.*\\n?\\r?'
     whitespace = #'[\\s\\n\\r]+'
     <word> = #'[\\p{Lu}\\p{Ll}\\d._-]+'
"))

(defn numeric [token] 
  (if (and (= -1 (.indexOf token "."))
           (= -1 (.indexOf token "E")))
    (java.lang.Long/parseLong token)
    (java.lang.Double/parseDouble token)))

(defn unify-choice
  ([c]
    (unify-choice c 1)) 
  ([c w]
    (vector :choice w c)))

(def transforms {:number numeric :symbol identity :string str :choice unify-choice})

(defn third [c] (nth c 2))

(declare simplify)
(defn simplify-1 [tree] (simplify tree 1))

(defn simplify-range 
  "Turns a complicated range (more than two items) into a list 
  of simple ranges (two items) weighted by their area"
  [items pweight]
  (let [area-of (fn [pair] (first (apply area pair)))
        to-range (fn [pair] {:type :range 
                             :weight (area-of pair)
                             :items pair})
        ranges (->>
                 (map simplify-1 items)
                 (partition 2 1)
                 (map to-range))
        wtree (weighted-tree ranges :weight)]
    {:type :list :weight pweight :sum (:sum wtree)
     :wtree wtree}))

(def get-weight second)
(defmulti simplify (fn [arr pweight] 
                     (do 
                       ;(prn (:type arr) r) 
                       (first arr))))

(defmethod simplify :choice [arr pweight]
  (let [subarr (third arr)
        weight (get-weight arr)] 
    (if (vector? subarr)
      (simplify subarr weight)
      {:type :choice :weight weight :item subarr})))

(defmethod simplify :list [arr pweight]
  (let [items (rest arr)
        wtree (weighted-tree (map simplify-1 items) :weight)]
    {:type :list :weight pweight :sum (:sum wtree)
     :wtree wtree}))
 
(defmethod simplify :range [arr pweight]
  (let [items (rest arr)]
    (if (>= 2 (count items))
      {:type :range :weight pweight :items (map simplify-1 items)}
      (simplify-range items pweight))))

(defmethod simplify :field [arr pweight]
  {(keyword (second arr)) (simplify (third arr) 1)})

(defmethod simplify :fieldref [arr pweight]
  {:type :field 
   :field (keyword (second arr))})

(defmethod simplify :fields [arr pweight]
  (into {} (map simplify-1 (rest arr))))
 
(defmethod simplify :function  [arr pweight]
  (let [items (rest (rest arr))
        name (second arr)
        func (or (ns-resolve 'fabulate.dslfunctions (symbol name)) 
                 (ns-resolve 'clojure.core (symbol name)))]
    {:type :function :weight pweight :name name :fn func :params (map simplify-1 items)}))

(defmethod simplify :default [arr pweight]
  (throw (IllegalArgumentException. (format "simplification of key %s unknown" (first arr)))))

(defn parse 
  ([start-rule dsl]
    (let [tree (choice-parser dsl :start start-rule)]
      (if (insta/failure? tree)
        (insta/get-failure tree) 
        (->> tree
          (insta/transform transforms)
          (simplify-1))))))
