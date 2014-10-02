(ns fabulate.parsing
  (:use fabulate.core)
  (:use fabulate.range)
  (:require [clojure.walk])
  (:require [fabulate.dslfunctions :as dsl])
  (:require [instaparse.core :as insta])
  (:require [re-rand.parser.rules :as rr]))

(def dsl-parser (insta/parser
    "prototypes = (<newline> <noise>?)* prototype ((<newline> <noise>?)+ prototype)* (<newline>? <noise>?)*
     prototype = (<newline> <noise>?)* <'prototype'> <noise>? symbol (<newline>? <noise>?)* fieldblock
     fieldblock = <'{'> fields (<newline>? <noise>?)* <'}'>
     fields = (<newline> <noise>?)* field ((<newline> <noise>?)+ field)* <newline>?
     field = <noise>? symbol <noise>? ( choice | function ) <noise>?
     choice = simple-choice ( <':'> <noise>? number )?
     function = <noise>? symbol (<noise>? choice)*
     <simple-choice> = string | symbol | number | regex | list | range | paren-function | fieldref | fieldblock
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
  [items pweight ctx]
  (let [area-of (fn [pair] (first (apply area pair)))
        to-range (fn [pair] {:type :range
                             :weight (area-of pair)
                             :items pair})
        ranges (->>
                 (map simplify items)
                 (partition 2 1)
                 (map to-range))
        wtree (weighted-tree ranges :weight)]
    {:type :list :weight pweight :sum (:sum wtree) :wtree wtree}))

(defn with-ctx [ctx m]
  (if (empty? ctx)
    m
    (assoc m :ctx ctx)))

(defmulti simplify
  (fn
    ([items]) ; returns nil, and will end up at the :default handler
    ([items pweight ctx]
     (first items))))

(defmethod simplify :choice [items pweight ctx]
  (let [[kw weight choice] items]
    (with-ctx ctx
              (if (vector? choice)
                (simplify choice weight ctx)
                {:type kw :weight weight :item choice}))))

(defmethod simplify :list [items pweight ctx]
  (let [items (rest items)
        wtree (weighted-tree (map simplify items) :weight)]
    (with-ctx ctx {:type  :list :weight pweight :sum (:sum wtree)
                   :wtree wtree})))

(defmethod simplify :range [items pweight ctx]
  (let [items (rest items)]
    (with-ctx ctx (if (>= 2 (count items))
                    {:type :range :weight pweight :items (map simplify items)}
                    (simplify-range items pweight ctx)))))

(defmethod simplify :field [items pweight ctx]
  (let [[kw name choice] items
        namekw (keyword name)
        newctx (conj ctx namekw)]
    {(keyword name) (simplify choice pweight newctx)}))

(defmethod simplify :fieldref [items pweight ctx]
  (let [[kw name] items]
    (with-ctx ctx {:type :fieldref :weight pweight :name name})))

(defmethod simplify :fields [items pweight ctx]
  (into {} (map #(simplify % pweight ctx) (rest items))))

(defmethod simplify :prototype [items pweight ctx]
  (let [[kw name fieldblock] items
        namekw (keyword name)]
    (with-ctx ctx {namekw (simplify fieldblock 1 ctx) :type kw :weight pweight})))

(defmethod simplify :fieldblock [items pweight ctx]
  (let [[kw fields] items]
    (with-ctx ctx {:type :prototype :weight pweight :fields (simplify fields pweight ctx)})))

(defmethod simplify :prototypes [items pweight ctx]
  (into {} (map simplify (rest items))))

(defmethod simplify :function  [items pweight ctx]
  (let [[kw name & items] items
        func (or (ns-resolve 'fabulate.dslfunctions (symbol name))
                 (ns-resolve 'clojure.core (symbol name)))]
    (with-ctx ctx {:type :function :weight pweight :name name :fn func :params (map simplify items)})))

(defmethod simplify :regex [items pweight ctx]
  (let [[kw pattern] items]
    (with-ctx ctx {:type :regex :weight pweight :generator (first (rr/pattern (str pattern))) :pattern pattern})))

(defmethod simplify :default
  ([items] (simplify items 1 []))
  ([items pweight ctx]
   (throw (IllegalArgumentException. (format "simplification of key %s unknown" (first items))))))

(defn- update-xref [fields]
  (fn [field]
    (if (= :fieldref (:type field))
      (assoc field :xref (lookup-field (:name field) fields))
      field)))

(defn resolve-xrefs [fields]
  (clojure.walk/prewalk (update-xref fields) fields))


(defn parse
  ([start-rule dsl]
    (let [tree (dsl-parser dsl :start start-rule)]
      (if (insta/failure? tree)
        (insta/get-failure tree)
        (-> (insta/transform transforms tree)
            (simplify 1 [])
            (resolve-xrefs))))))
