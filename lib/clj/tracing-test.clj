(ns clj.__tracing-test__
  (:require [clojure.test :refer :all]
            [clj.__tracing__ :as t]
            [com.billpiel.sayid.core :as sayid]))

(defn test1 [x y]
  (let [a (+ x y)
        b (- x y)]
    (+ a b)))

(defn test2 [a]
  (let [b (test1 a 10)]
    (+ b 10)))

(defn reset-all []
  (sayid/ws-get-active!)
  (sayid/ws-reset!))

(testing "tracing a single function"
  (reset-all)
  (sayid/ws-add-trace-fn! test1)
  (test1 11 20)
  (is (= [{:fn "clj.__tracing-test__/test1"
           :args ["11" "20"]
           :returned "22"
           :children nil}]
         (t/trace-str))))
