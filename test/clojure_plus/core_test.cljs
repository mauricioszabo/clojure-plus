(ns clojure-plus.core-test
  (:require [clojure.test :refer-macros [run-tests testing is deftest async]]
            [clojure-plus.core :as core]))

(deftest running-code
  (async done
   (testing "runs code on NREPL connection"
     (core/execute-cmd '(+ 2 1)
                       "user"
                       (fn [res]
                         (is (= {:value 3} res))
                         (done))))))
(run-tests)
