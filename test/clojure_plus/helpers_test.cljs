(ns ^:figwheel-always clojure-plus.helpers-test
  ; (:require-macros [clojure.test :refer [deftest testing is run-tests]])
  (:require ;[clojure-plus.test :refer [run-tests]]
            ; [cljs.test :as t]
            [cljs.test :refer-macros [deftest is testing run-tests]]

            [clojure-plus.helpers :as helpers]
            [clojure-plus.core :as core]))

(def foo-element (.createElement js/document "foo"))

(defn find-commands [name]
  (-> js/atom
      .-commands
      (.findCommands #js {:target foo-element})
      js->clj
      (->> (filter #(= (% "displayName") name)))))

(deftest foobar
  (testing "FooBar"
    (is (= "Foo" "Foo"))))


(set! js/__dirname (str (.resolve (js/require "path") ".")
                        "/lib/js/foo/bar"))
(run-tests)
;
; (do ; Add commands
;   (helpers/add-command "foo" "Sample Test" #(println))
;   (testing "command is on global"
;     #(not-empty (find-commands "Sample Test")))
;
;   (helpers/remove-all-commands)
;   (testing "removes command"
;     #(empty? (find-commands "Sample Test"))))
