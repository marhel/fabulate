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

(defn simplify-range 
  "Turns a complicated range (more than two items) into a list 
  of simple ranges (two items) weighted by their area"
  [items pweight]
  (let [area-of (fn [pair] (first (apply area pair)))
        to-range (fn [pair] {:type :range 
                             :weight (area-of pair)
                             :items pair})
        ranges (->>
                 (map simplify items)
                 (partition 2 1)
                 (map to-range))
        wtree (weighted-tree ranges :weight)]
    {:type :list :weight pweight :sum (:sum wtree)
     :wtree wtree}))

(defmulti simplify 
  (fn 
    ([items]) ; returns nil, and will end up at the :default handler
    ([items pweight] 
      (do 
        ;(prn (:type arr) r) 
        (first items)))))

(defmethod simplify :choice [items pweight]
  (let [[kw weight choice] items]
    (if (vector? choice)
      (simplify choice weight)
      {:type kw :weight weight :item choice})))

(defmethod simplify :list [items pweight]
  (let [items (rest items)
        wtree (weighted-tree (map simplify items) :weight)]
    {:type :list :weight pweight :sum (:sum wtree)
     :wtree wtree}))
 
(defmethod simplify :range [items pweight]
  (let [items (rest items)]
    (if (>= 2 (count items))
      {:type :range :weight pweight :items (map simplify items)}
      (simplify-range items pweight))))

(defmethod simplify :field [items pweight]
  (let [[kw name choice] items]
    {(keyword name) (simplify choice pweight)}))

(defmethod simplify :fieldref [items pweight]
  (let [[kw name] items]
    {:type :field, :field (keyword name)}))

(defmethod simplify :fields [items pweight]
  (into {} (map simplify (rest items))))
 
(defmethod simplify :function  [items pweight]
  (let [[kw name & items] items
        func (or (ns-resolve 'fabulate.dslfunctions (symbol name))
                 (ns-resolve 'clojure.core (symbol name)))]
    {:type :function :weight pweight :name name :fn func :params (map simplify items)}))

(defmethod simplify :default
  ([items] (simplify items 1))
  ([items pweight]
    (throw (IllegalArgumentException. (format "simplification of key %s unknown" (first items))))))

(defn parse 
  ([start-rule dsl]
    (let [tree (choice-parser dsl :start start-rule)]
      (if (insta/failure? tree)
        (insta/get-failure tree)
        (simplify (insta/transform transforms tree) 1)))))
