(ns clojure-plus.helpers-test
  (:require [clojure-plus.test :refer [testing]]
            [clojure-plus.helpers :as helpers]
            [clojure-plus.core :as core]))

(def foo-element (.createElement js/document "foo"))

(defn find-commands [name]
  (-> js/atom
      .-commands
      (.findCommands #js {:target foo-element})
      js->clj
      (->> (filter #(= (% "displayName") name)))))

(do ; Add commands
  (helpers/add-command "foo" "Sample Test" #(println))
  (testing "command is on global"
    #(not-empty (find-commands "Sample Test")))

  (helpers/remove-all-commands)
  (testing "removes command"
    #(empty? (find-commands "Sample Test"))))
