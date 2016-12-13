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

(defn test3 [x y]
  (when-let [c (inc x)]
    (+ c y)))

(defn reset-all []
  (reset! sayid/workspace nil)
  (sayid/ws-get-active!)
  (with-out-str (sayid/ws-reset!)))

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
  (is (keyword? (-> (t/trace-str) first :id keyword))))

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

(testing "reset"
  (reset-all)
  (sayid/ws-add-trace-fn! test1)
  (test1 11 20)
  (t/reset-sayid!)
  (is (empty? (t/trace-str)))
  (test1 11 20)
  (is (not (empty? (t/trace-str)))))

(testing "inner trace"
  (reset-all)
  (sayid/ws-add-trace-fn! test3)
  (test3 11 20)
  (let [id (-> (t/trace-str) first :id keyword)]
    (is (= [{:fn "when-let"
             :args nil
             :mapping nil
             :returned 32
             :children [{:fn "inc"
                         :args ["11"]
                         :mapping nil
                         :returned 12
                         :children nil}
                        {:fn "+"
                         :args ["12" "20"]
                         :mapping nil
                         :returned 32
                         :children nil}]}]
           (traced (t/trace-inner id))))))

(testing "inner trace with let"
  (reset-all)
  (sayid/ws-add-trace-fn! test1)
  (test1 11 20)
  (test1 22 20)
  (let [id (-> (t/trace-str) first :id keyword)]
    (is (= [{:fn "let"
             :args ["[a 31 b -9]"]
             :mapping {'a 31 'b -9}
             :returned 22
             :children [{:fn "(+ x y)"
                         :returned 31
                         :children nil}
                        {:fn "(- x y)"
                         :returned -9
                         :children nil}
                        {:fn "+"
                         :args ["31" "-9"]
                         :mapping nil
                         :returned 22
                         :children nil}]}]
           (traced (t/trace-inner id))))))
