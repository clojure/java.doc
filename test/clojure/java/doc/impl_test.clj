(ns clojure.java.doc.impl-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [clojure.java.doc.impl :as sut])
  (:import [org.jsoup Jsoup]))

(deftest get-method-detail-test
  (testing "Getting method detail from HTML"
    (let [html "<html><body>
                <section id='valueOf(int)'>
                  <h3>valueOf</h3>
                  <div class='member-signature'>
                    <span>public static String valueOf(int i)</span>
                  </div>
                  <div class='block'>Returns the string representation of the int argument.</div>
                </section>
                <section id='valueOf(char[])'>
                  <h3>valueOf</h3>
                  <div class='member-signature'>
                    <span>public static String valueOf(char[] data)</span>
                  </div>
                  <div class='block'>Returns the string representation of the char array argument.</div>
                </section>
                </body></html>"
          doc (Jsoup/parse html)]

  (testing "Finds correct method by exact signature match"
    (let [method {:signature "valueOf(int i)" :description "test"}
          actual (#'sut/get-method-detail doc method)
          expected-html "<section id=\"valueOf(int)\">
 <h3>valueOf</h3>
 <div class=\"member-signature\">
  <span>public static String valueOf(int i)</span>
 </div>
 <div class=\"block\">
  Returns the string representation of the int argument.
 </div>
</section>"]
     (is (= "valueOf(int i)" (:signature actual)))
     (is (= "test" (:description actual)))
     (is (= expected-html (:method-description-html actual)))))

  (testing "Finds correct overload with array parameter"
    (let [method {:signature "valueOf(char[] data)" :description "test"}
          actual (#'sut/get-method-detail doc method)
          expected-html "<section id=\"valueOf(char[])\">
 <h3>valueOf</h3>
 <div class=\"member-signature\">
  <span>public static String valueOf(char[] data)</span>
 </div>
 <div class=\"block\">
  Returns the string representation of the char array argument.
 </div>
</section>"]
          (is (= "valueOf(char[] data)" (:signature actual)))
           (is (= "test" (:description actual)))
           (is (= expected-html (:method-description-html actual)))))

  (testing "Returns original method when detail not found"
    (let [method {:signature "nonExistent(int x)" :description "test"}
          actual (#'sut/get-method-detail doc method)]
      (is (= method actual)))))))

(deftest resolve-class-name-test
  (testing "java.lang class resolves without import"
    (is (= "java.lang.Integer" (#'sut/resolve-class-name "Integer")))
    (is (= "java.lang.Object" (#'sut/resolve-class-name "Object"))))

  (testing "unimported class throws exception"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Cannot resolve class: HashMap"
                          (#'sut/resolve-class-name "HashMap"))))

  (testing "fully qualified class resolves"
    (is (= "java.util.HashMap" (#'sut/resolve-class-name "java.util.HashMap"))))

  (testing "unresolveable throws exception"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Cannot resolve class: com.example.MyClass"
                          (#'sut/resolve-class-name "com.example.MyClass")))))

(deftest extract-params-test

  (testing "single parameter"
    (is (= ["int"] (#'sut/extract-params "valueOf(int i)"))))

  (testing "array parameter"
    (is (= ["char[]"] (#'sut/extract-params "valueOf(char[] data)"))))

  (testing "no parameters"
    (is (nil? (#'sut/extract-params "length()"))))

  (testing "type with generics stripped"
    (is (= ["List"] (#'sut/extract-params "addAll(List<String> items)"))))

  (testing "generics with multiple type parameters"
    (is (= ["java.util.Map"] (#'sut/extract-params "run(java.util.Map<java.lang.String, ? extends OnnxTensorLike> inputs)"))))

  (testing "multiple params with generics"
    (is (= ["String" "Map" "int"] (#'sut/extract-params "process(String name, Map<K,V> data, int count)"))))

  (testing "nested generics"
    (is (= ["Map"] (#'sut/extract-params "transform(Map<String,List<Integer>> data)"))))

  (testing "multiple params with nested generics"
    (is (= ["java.util.Map" "java.util.Map"]
           (#'sut/extract-params "run(java.util.Map<java.lang.String, ? extends OnnxTensorLike> inputs, java.util.Map<java.lang.String, ? extends OnnxValue> pinnedOutputs)"))))

  (testing "array types"
    (is (= ["int[]"] (#'sut/extract-params "sort(int[] array)"))))

  (testing "varargs"
    (is (= ["String" "Object..."] (#'sut/extract-params "format(String format, Object... args)")))))

(deftest expand-array-syntax-test

  (testing "array syntax"
    (is (= "String[]" (#'sut/expand-array-syntax "String/1")))
    (is (= "String[][]" (#'sut/expand-array-syntax "String/2")))
    (is (= "CharSequence[]" (#'sut/expand-array-syntax "CharSequence/1")))
    (is (= "int[][][]" (#'sut/expand-array-syntax "int/3"))))

  (testing "varargs"
    (is (= "CharSequence[]" (#'sut/expand-array-syntax "CharSequence...")))
    (is (= "Object[]" (#'sut/expand-array-syntax "Object..."))))

  (testing "no transformation needed"
    (is (= "int" (#'sut/expand-array-syntax "int")))
    (is (= "String" (#'sut/expand-array-syntax "String")))
    (is (= "int[]" (#'sut/expand-array-syntax "int[]")))
    (is (= "String[][]" (#'sut/expand-array-syntax "String[][]")))))

(deftest compress-array-syntax-test

  (testing "primitive types unchanged"
    (is (= "int" (#'sut/compress-array-syntax "int")))
    (is (= "String" (#'sut/compress-array-syntax "String"))))

  (testing "single dimension arrays"
    (is (= "char/1" (#'sut/compress-array-syntax "char[]")))
    (is (= "String/1" (#'sut/compress-array-syntax "String[]")))
    (is (= "int/1" (#'sut/compress-array-syntax "int[]"))))

  (testing "multi-dimension arrays"
    (is (= "String/2" (#'sut/compress-array-syntax "String[][]")))
    (is (= "int/3" (#'sut/compress-array-syntax "int[][][]"))))

  (testing "varargs"
    (is (= "Object/1" (#'sut/compress-array-syntax "Object...")))
    (is (= "String/1" (#'sut/compress-array-syntax "String...")))))

(deftest clojure-call-syntax-test

  (testing "instance method with single parameter"
    (is (= "^[CharSequence] String/.contains"
           (#'sut/clojure-call-syntax "String" "contains(CharSequence s)" false))))

  (testing "instance method with multiple parameters"
    (is (= "^[int int] String/.substring"
           (#'sut/clojure-call-syntax "String" "substring(int beginIndex, int endIndex)" false))))

  (testing "instance method with no parameters"
    (is (= "String/.length"
           (#'sut/clojure-call-syntax "String" "length()" false))))

  (testing "static method with array parameters"
    (is (= "^[char/1] String/valueOf"
           (#'sut/clojure-call-syntax "String" "valueOf(char[] data)" true))))

  (testing "static method with varargs"
    (is (= "^[String Object/1] String/format"
           (#'sut/clojure-call-syntax "String" "format(String format, Object... args)" true))))

  (testing "instance method with arrays"
    (is (= "^[String/2] SomeClass/.someMethod"
           (#'sut/clojure-call-syntax "SomeClass" "someMethod(String[][] data)" false))))

  (testing "static method with no parameters"
    (is (= "SomeClass/staticMethod"
           (#'sut/clojure-call-syntax "SomeClass" "staticMethod()" true)))))

(deftest method-matches-test

  (testing "method name matching"
    (is (true? (#'sut/method-matches? "valueOf(int i)" "valueOf" nil)))
    (is (true? (#'sut/method-matches? "valueOf(char[] data)" "valueOf" nil)))
    (is (false? (#'sut/method-matches? "charAt(int index)" "valueOf" nil))))

  (testing "method name with param-tags"
    (is (true? (#'sut/method-matches? "valueOf(int i)" "valueOf" '[int])))
    (is (true? (#'sut/method-matches? "valueOf(char[] data)" "valueOf" '[char/1])))
    (is (false? (#'sut/method-matches? "valueOf(int i)" "valueOf" '[char/1])))
    (is (false? (#'sut/method-matches? "valueOf(char[] data)" "valueOf" '[int]))))

  (testing "method name matches but params do not"
    (is (false? (#'sut/method-matches? "valueOf(int i)" "valueOf" '[String])))
    (is (false? (#'sut/method-matches? "contains(CharSequence s)" "contains" '[String]))))

  (testing "array syntax"
    (is (true? (#'sut/method-matches? "valueOf(char[] data)" "valueOf" '[char/1])))
    (is (true? (#'sut/method-matches? "copyValueOf(char[] data, int offset, int count)" "copyValueOf" '[char/1 int int])))
    (is (true? (#'sut/method-matches? "format(String format, Object... args)" "format" '[String Object/1])))
    (is (false? (#'sut/method-matches? "format(String format, Object... args)" "format" '[String Object/2]))))

  (testing "with wildcards"
    (is (true? (#'sut/method-matches? "valueOf(int i)" "valueOf" '[_])))
    (is (true? (#'sut/method-matches? "copyValueOf(char[] data, int offset, int count)" "copyValueOf" '[_ int int])))
    (is (true? (#'sut/method-matches? "copyValueOf(char[] data, int offset, int count)" "copyValueOf" '[char/1 _ int])))))

(deftest params-match-test

  (testing "single parameter"
    (is (true? (#'sut/params-match? ["int"] '[int]))))

  (testing "multiple parameters"
    (is (true? (#'sut/params-match? ["int" "int"] '[int int]))))

  (testing "wrong count too few param-tags"
    (is (false? (#'sut/params-match? ["int" "int"] '[int]))))

  (testing "wrong count too many param-tags"
    (is (false? (#'sut/params-match? ["int"] '[int int]))))

  (testing "correct number, but wrong type"
    (is (false? (#'sut/params-match? ["String"] '[int]))))

  (testing "one of many is wrong"
    (is (false? (#'sut/params-match? ["int" "String"] '[int int]))))

  (testing "no args in signature"
    (is (nil? (#'sut/params-match? nil '[int]))))

  (testing "single dimension"
    (is (true? (#'sut/params-match? ["int[]"] '[int/1])))
    (is (true? (#'sut/params-match? ["CharSequence[]"] '[CharSequence/1]))))

  (testing "multiple dimensions"
    (is (true? (#'sut/params-match? ["String[][]"] '[String/2]))))

  (testing "varargs matches array syntax"
    (is (true? (#'sut/params-match? ["CharSequence..."] '[CharSequence/1]))))

  (testing "wildcards"
    (is (true? (#'sut/params-match? ["int"] '[_])))
    (is (true? (#'sut/params-match? ["String"] '[_])))
    (is (true? (#'sut/params-match? ["int" "String"] '[_ _])))
    (is (true? (#'sut/params-match? ["int" "String"] '[_ String])))
    (is (true? (#'sut/params-match? ["int" "String"] '[int _])))
    (is (false? (#'sut/params-match? ["int" "String"] '[_])))
    (is (true? (#'sut/params-match? ["CharSequence..." "CharSequence..."] '[_ CharSequence/1])))))

(deftest extract-id-params-test

  (testing "no parameters"
    (is (= [] (#'sut/extract-id-params "valueOf()" "valueOf"))))

  (testing "single parameter"
    (is (= ["int"] (#'sut/extract-id-params "valueOf(int)" "valueOf"))))

  (testing "single fully qualified parameter"
    (is (= ["java.lang.String"] (#'sut/extract-id-params "valueOf(java.lang.String)" "valueOf"))))

  (testing "multiple simple parameters"
    (is (= ["int" "int"] (#'sut/extract-id-params "substring(int,int)" "substring"))))

  (testing "multiple fully qualified parameters"
    (is (= ["java.util.Map" "java.util.Set"] (#'sut/extract-id-params "run(java.util.Map,java.util.Set)" "run"))))

  (testing "array parameters"
    (is (= ["char[]"] (#'sut/extract-id-params "valueOf(char[])" "valueOf"))))

  (testing "mixed simple and qualified parameters"
    (is (= ["java.util.Map" "int"] (#'sut/extract-id-params "process(java.util.Map,int)" "process"))))

  (testing "inner class parameters with dot separator"
    (is (= ["ai.onnxruntime.OrtSession.RunOptions"]
           (#'sut/extract-id-params "run(ai.onnxruntime.OrtSession.RunOptions)" "run"))))

  (testing "non-matching method name returns nil"
    (is (nil? (#'sut/extract-id-params "valueOf(int)" "substring")))))

(deftest params-match-id-test

  (testing "exact match with simple types"
    (is (true? (#'sut/params-match-id? ["int"] ["int"])))
    (is (true? (#'sut/params-match-id? ["int" "String"] ["int" "String"]))))

  (testing "simple type matches fully qualified type"
    (is (true? (#'sut/params-match-id? ["String"] ["java.lang.String"])))
    (is (true? (#'sut/params-match-id? ["Map"] ["java.util.Map"])))
    (is (true? (#'sut/params-match-id? ["Set"] ["java.util.Set"]))))

  (testing "multiple params with mixed simple and qualified"
    (is (true? (#'sut/params-match-id? ["Map" "Set"] ["java.util.Map" "java.util.Set"])))
    (is (true? (#'sut/params-match-id? ["String" "int"] ["java.lang.String" "int"]))))

  (testing "array types match"
    (is (true? (#'sut/params-match-id? ["char[]"] ["char[]"])))
    (is (true? (#'sut/params-match-id? ["String[]"] ["java.lang.String[]"]))))

  (testing "inner class with dot separator"
    (is (true? (#'sut/params-match-id? ["OrtSession.RunOptions"] ["ai.onnxruntime.OrtSession.RunOptions"]))))

  (testing "simple inner class name matches fully qualified"
    (is (true? (#'sut/params-match-id? ["RunOptions"] ["ai.onnxruntime.OrtSession.RunOptions"]))))

  (testing "wrong type does not match"
    (is (false? (#'sut/params-match-id? ["int"] ["String"])))
    (is (false? (#'sut/params-match-id? ["Map"] ["java.util.Set"]))))

  (testing "wrong count does not match"
    (is (false? (#'sut/params-match-id? ["int"] ["int" "String"])))
    (is (false? (#'sut/params-match-id? ["int" "String"] ["int"]))))

  (testing "empty params match"
    (is (true? (#'sut/params-match-id? [] [])))))

(deftest find-method-section-test

  (testing "finds method with simple type parameters"
    (let [html "<html><body>
                <section id='valueOf(int)'>
                  <h3>valueOf</h3>
                </section>
                <section id='valueOf(char[])'>
                  <h3>valueOf</h3>
                </section>
                </body></html>"
          doc (Jsoup/parse html)
          section-int (#'sut/find-method-section doc "valueOf" ["int"])
          section-char-array (#'sut/find-method-section doc "valueOf" ["char[]"])]
      (is (= "valueOf(int)" (.attr section-int "id")))
      (is (= "valueOf(char[])" (.attr section-char-array "id")))))

  (testing "finds method with fully qualified type parameters"
    (let [html "<html><body>
                <section id='run(java.util.Map)'>
                  <h3>run</h3>
                </section>
                <section id='run(java.util.Map,java.util.Set)'>
                  <h3>run</h3>
                </section>
                </body></html>"
          doc (Jsoup/parse html)
          section-map (#'sut/find-method-section doc "run" ["Map"])
          section-map-set (#'sut/find-method-section doc "run" ["Map" "Set"])]
      (is (= "run(java.util.Map)" (.attr section-map "id")))
      (is (= "run(java.util.Map,java.util.Set)" (.attr section-map-set "id")))))

  (testing "finds method with no parameters, nil from extract-params"
    (let [html "<html><body>
                <section id='length()'>
                  <h3>length</h3>
                </section>
                </body></html>"
          doc (Jsoup/parse html)
          section (#'sut/find-method-section doc "length" nil)]
      (is (= "length()" (.attr section "id")))))

  (testing "finds method with no parameters, empty vector"
    (let [html "<html><body>
                <section id='toString()'>
                  <h3>toString</h3>
                </section>
                </body></html>"
          doc (Jsoup/parse html)
          section (#'sut/find-method-section doc "toString" [])]
      (is (= "toString()" (.attr section "id")))))

  (testing "returns nil when method not found"
    (let [html "<html><body>
                <section id='valueOf(int)'>
                  <h3>valueOf</h3>
                </section>
                </body></html>"
          doc (Jsoup/parse html)]
      (is (nil? (#'sut/find-method-section doc "valueOf" ["String"])))
      (is (nil? (#'sut/find-method-section doc "nonExistent" ["int"])))))

  (testing "distinguishes between overloads with different param counts"
    (let [html "<html><body>
                <section id='run(java.util.Map)'>
                  <h3>run</h3>
                </section>
                <section id='run(java.util.Map,java.util.Set)'>
                  <h3>run</h3>
                </section>
                <section id='run(java.util.Map,java.util.Set,ai.onnxruntime.OrtSession.RunOptions)'>
                  <h3>run</h3>
                </section>
                </body></html>"
          doc (Jsoup/parse html)
          section-1 (#'sut/find-method-section doc "run" ["Map"])
          section-2 (#'sut/find-method-section doc "run" ["Map" "Set"])
          section-3 (#'sut/find-method-section doc "run" ["Map" "Set" "OrtSession.RunOptions"])]
      (is (= "run(java.util.Map)" (.attr section-1 "id")))
      (is (= "run(java.util.Map,java.util.Set)" (.attr section-2 "id")))
      (is (= "run(java.util.Map,java.util.Set,ai.onnxruntime.OrtSession.RunOptions)" (.attr section-3 "id"))))))
