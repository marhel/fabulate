(ns fabulate.parsing
  (:use fabulate.core)
  (:use fabulate.range)
  (:require [fabulate.dslfunctions :as dsl])
  (:require [instaparse.core :as insta]))

(def choice-parser (insta/parser
    "field = symbol <noise>? ( choice | function ) <noise>?
     choice = simple-choice <noise>? ( <':'> <noise>? number )?
     list = <noise>? <'{'> (<noise>? choice)+ <noise>? <'}'>
     range = <noise>? <'['> (<noise>? &number choice)+ <noise>? <']'>
     symbol = !number word
     function = <noise>? symbol (<noise>? choice)*
     <paren-function> = <noise>? <'('> function <noise>? <')'>
     <simple-choice> = string | symbol | number | list | range | paren-function
     string = <noise>? <quote> (!quote !escape #'.' | escaped-char)* <quote>
     <escaped-char> = escape any-or-newlines
     <quote> = '\\\"'
     <escape> = <'\\\\'>
     <any-or-newlines> = #'(.|\\n\\r?)'
     noise = (<comment> | <whitespace>)+
     comment = #'#.*\\n?\\r?'
     whitespace = #'[\\s\\n\\r]+'
     <word> = #'[\\p{Lu}\\p{Ll}\\d._-]+'
     number = #'[-+]?[0-9][.\\w]*'"))

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

(defn simplify 
  ([tree]
    (simplify tree 1))
  ([tree pweight]
    (let [this-key (first tree)
          get-weight second]
      (case this-key
        :choice (let [subtree (third tree)
                      weight (get-weight tree)] 
                  (if (vector? subtree)
                    (recur subtree weight)
                    {:type this-key :weight weight :item subtree}))
        :list (let [items (rest tree)
                    wtree (weighted-tree (map simplify items) :weight)]
                {:type this-key :weight pweight :sum (:sum wtree)
                 :wtree wtree})
        :range (let [items (rest tree)]
                 (if (>= 2 (count items))
                   {:type this-key :weight pweight :items (map simplify items)}
                   (simplify-range items pweight)))
        :field {(keyword (second tree)) (simplify (third tree))}
        :function (let [items (rest (rest tree))
                        name (second tree)
                        func (or (ns-resolve 'fabulate.dslfunctions (symbol name)) 
                                 (ns-resolve 'clojure.core (symbol name)))]
                    {:type this-key :weight pweight :name name :fn func :params (map simplify items)})
        (throw (IllegalArgumentException. (format "simplification of key %s unknown" this-key)))))))

(defn parse 
  ([dsl]
    (parse dsl :field)) 
  ([dsl start-rule]
  (let [tree (choice-parser dsl :start start-rule)]
    (if (insta/failure? tree)
      (insta/get-failure tree) 
      (->> tree
        (insta/transform transforms)
        (simplify))))))
