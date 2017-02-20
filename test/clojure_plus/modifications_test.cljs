(ns clojure-plus.modifications-test
  (:require [clojure-plus.test :refer [testing]]
            [clojure-plus.modifications :as mods]))

(def clj-text "
(let [a (+ b c)]
  (inc a))
")

(do ; Rewrite sexps
  (testing "rewriting SEXP from some code"
    #(= "
(let [a (+ (println b) (pp c))]
  (inc a))
"
        (mods/rewrite-txt clj-text
                          [{:replace "(println __SEL__)" :start 12 :end 13}
                           {:replace "(pp __SEL__)" :start 14 :end 15}]))))

(subs clj-text 13)

(binding [a *out*]
    (println "h")
    a)
