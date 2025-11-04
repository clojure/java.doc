(ns clojure.java.doc.api
  (:require
    [clojure.java.doc.impl :refer [parse-javadoc print-javadoc print-signatures]]))

(defn javadoc-data-fn [s param-tags]
  (parse-javadoc s param-tags))

(defn javadoc-fn [s param-tags]
  (print-javadoc (javadoc-data-fn s param-tags)))

(defmacro jdoc-data
  "a map containg javadoc data for a class or method
   Examples:
     (jdoc-data String)                   ; Get class data
     (jdoc-data String/valueOf)           ; Get data for all valueOf overloads
     (jdoc-data ^[char/1] String/valueOf) ; Get data for specific overload"
  [class-or-method]
  `(javadoc-data-fn ~(str class-or-method) '~(:param-tags (meta class-or-method))))

(defmacro jdoc
  "print javadoc for a class or method
   Examples:
     (jdoc String)                    ; Print class description
     (jdoc String/valueOf)            ; Print all valueOf overloads
     (jdoc ^[char/1] String/valueOf)  ; Print specific overload"
  [class-or-method]
  `(javadoc-fn ~(str class-or-method) '~(:param-tags (meta class-or-method))))

(defn sigs-fn [s param-tags]
  (print-signatures (javadoc-data-fn s param-tags)))

(defmacro sigs
  "print method signatures in qualified method / param tags form
   Examples:
     (sigs String/valueOf)  ; Print all valueOf signatures"
  [class-or-method]
  `(sigs-fn ~(str class-or-method) '~(:param-tags (meta class-or-method))))
