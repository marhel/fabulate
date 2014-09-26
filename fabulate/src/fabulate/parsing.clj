(ns fabulate.parsing
  (:use fabulate.core)
  (:use fabulate.range)
  (:require [fabulate.dslfunctions :as dsl])
  (:require [instaparse.core :as insta])
  (:require [re-rand.parser.rules :as rr]))

(def dsl-parser (insta/parser
    "prototypes = (<newline> <noise>?)* prototype ((<newline> <noise>?)+ prototype)* <newline>?
     prototype = (<newline> <noise>?)* <'prototype'> <noise>? symbol (<newline>? <noise>?)* <'{'> fields (<newline>? <noise>?)* <'}'>
     fields = (<newline> <noise>?)* field ((<newline> <noise>?)+ field)* <newline>?
     field = <noise>? symbol <noise>? ( choice | function ) <noise>? 
     choice = simple-choice ( <':'> <noise>? number )?
     function = <noise>? symbol (<noise>? choice)*
     <simple-choice> = string | symbol | number | regex | list | range | paren-function | fieldref
     string = <noise>? <quote> (!quote !escape #'.' | escaped-char)* <quote>
     regex = <noise>? <'/'> (!'/' #'.' | <'\\\\'> '/' )* <'/'>
     symbol = !number word
     number = #'[-+]?[0-9][.\\w]*'
     list = <noise>? <'<'> (<noise>? choice)+ <noise>? <'>'>
     range = <noise>? <'['> (<noise>? &number choice)+ <noise>? <']'>
     <paren-function> = <noise>? <'('> function <noise>? <')'>
     fieldref = <'$'> symbol
     <escaped-char> = escape any-char
     <quote> = '\\\"'
     <escape> = <'\\\\'>
     <newline> = #'[\\n\\r]+'
     <any-char> = #'.'
     noise = (<comment> | <whitespace>)+
     comment = #'#.*'
     whitespace = #'[ \\t]+'
     <word> = #'[\\p{Lu}\\p{Ll}\\d._-]+'
"))

(defn numeric [token] 
  (if (and (= -1 (.indexOf token "."))
           (= -1 (.indexOf token "E")))
    (Long/parseLong token)
    (Double/parseDouble token)))

(defn unify-choice
  ([c]
    (unify-choice c 1)) 
  ([c w]
    (vector :choice w c)))
(defn make-rx [& v] [:regex (re-pattern (clojure.string/join v))])
(def transforms {:number numeric :symbol identity :string str :regex make-rx :choice unify-choice})

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
     ;(prn (:type arr) r)
     (first items))))

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
    {:type :field :weight pweight :field (keyword name)}))

(defmethod simplify :fields [items pweight]
  (into {} (map simplify (rest items))))

(defmethod simplify :prototype [items pweight]
  (let [[kw name fields] items]
    {(keyword name) (simplify fields) :type kw :weight pweight }))

(defmethod simplify :prototypes [items pweight]
  (into {} (map simplify (rest items))))

(defmethod simplify :function  [items pweight]
  (let [[kw name & items] items
        func (or (ns-resolve 'fabulate.dslfunctions (symbol name))
                 (ns-resolve 'clojure.core (symbol name)))]
    {:type :function :weight pweight :name name :fn func :params (map simplify items)}))

(defmethod simplify :regex [items pweight]
  (let [[kw pattern] items]
    {:type :regex :weight pweight :generator (first (rr/pattern (str pattern))) :pattern pattern}))

(defmethod simplify :default
  ([items] (simplify items 1))
  ([items pweight]
    (throw (IllegalArgumentException. (format "simplification of key %s unknown" (first items))))))

(defn parse 
  ([start-rule dsl]
    (let [tree (dsl-parser dsl :start start-rule)]
      (if (insta/failure? tree)
        (insta/get-failure tree)
        (simplify (insta/transform transforms tree) 1)))))