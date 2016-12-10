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

(defn traced [stacks]
  (when stacks (for [row stacks]
                 (-> row
                     (dissoc :id)
                     (update-in [:children] traced)))))

(testing "tracing a single function"
  (reset-all)
  (sayid/ws-add-trace-fn! test1)
  (test1 11 20)
  (is (= [{:fn "clj.__tracing-test__/test1"
           :args ["11" "20"]
           :returned 22
           :mapping {'x 11, 'y 20}
           :children nil}]
         (traced (t/trace-str))))
  (is (keyword? (-> (t/trace-str) first :id))))

(testing "tracing dependent functions"
  (reset-all)
  (sayid/ws-add-trace-fn! test1)
  (sayid/ws-add-trace-fn! test2)
  (test2 5)
  (is (= [{:fn "clj.__tracing-test__/test2"
           :args ["5"]
           :returned 20
           :mapping {'a 5}
           :children [{:fn "clj.__tracing-test__/test1"
                       :args ["5" "10"]
                       :returned 10
                       :mapping {'x 5, 'y 10}
                       :children nil}]}]
         (traced (t/trace-str)))))
