# java.doc

A Clojure library for accessing Javadocs in your REPL

## Installation

### deps.edn

```clojure
{:deps {org.clojure/java.doc {:git/url "https://github.com/clojure/java.doc"
                              :git/tag "v0.1.2"
                              :git/sha "fc518b1"}}}
```

### In the REPL with add-libs

For usage without modifying your project deps:

```clojure
;; This require is only necessary if not in user namespace
(require '[clojure.repl.deps :refer [add-lib]])

(add-lib 'io.github.clojure/java.doc {:git/tag "v0.1.2" :git/sha "fc518b1"})

(require '[clojure.java.doc.api :refer [jdoc jdoc-data sigs]])

;; Now you can use it
(jdoc String)
```

### From the Command Line

Invoke directly from the command line, useful for piping into a .md file to display in your editor:

```bash
clojure -Sdeps '{:deps {org.clojure/java.doc {:git/url "https://github.com/clojure/java.doc" :git/tag "v0.1.2" :git/sha "fc518b1"}}}' \
  -M -e "(require '[clojure.java.doc.api :refer [jdoc]]) (jdoc String)"
```

## Usage

The core namespace provides three functions:

### jdoc

Print Javadoc HTML as Markdown for a class or qualified method (with optional param-tags).

```clojure
(require '[clojure.java.doc.api :refer [jdoc jdoc-data sigs]])

;; Print class description
(jdoc String)

;; Print all overloads of a method
(jdoc String/valueOf)

;; Specify a specific overload using param-tags
(jdoc ^[char/1] String/valueOf)

;; Use _ to match any type:
(jdoc ^[_ int] String/.substring)
```

### sigs

Print method signatures in qualified method syntax with param tags.

```clojure
(sigs String/valueOf)
;; ^[boolean] String/valueOf
;; ^[char] String/valueOf
;; ^[char/1] String/valueOf
;; ^[char/1 int int] String/valueOf
;; ^[double] String/valueOf
;; ...
```

```clojure
(sigs java.util.UUID)
;; java.util.UUID/.clockSequence
;; ^[UUID] java.util.UUID/.compareTo
;; ^[Object] java.util.UUID/.equals
;; ^[String] java.util.UUID/fromString
;; java.util.UUID/.getLeastSignificantBits
;; java.util.UUID/.getMostSignificantBits
;; ...
```
### jdoc-data

Returns all the structured data instead of printing the description.

```clojure
(jdoc-data String)
;; => {:classname "java.lang.String"
;;     :class-description-html "..."
;;     :class-description-md "..."
;;     :methods [{:signature "valueOf(int i)"
;;                :description "Returns the string representation..."
;;                :static? true
;;                :clojure-call "^[int] String/valueOf"}
;;               ...]}

(jdoc-data ^[char/1] String/valueOf)
;; => {:classname "java.lang.String"
;;     :class-description-html "..."
;;     :class-description-md "..."
;;     :methods [...]
;;     :selected-method [{:signature "valueOf(char[] data)"
;;                        :description "Returns the string representation..."
;;                        :static? true
;;                        :clojure-call "^[char/1] String/valueOf"
;;                        :method-description-html "..."
;;                        :method-description-md "..."}]}
```

## Requirements

- Java 17+

## Copyright and License

Copyright © 2025

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
