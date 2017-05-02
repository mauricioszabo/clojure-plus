(ns ^:figwheel-always clojure-plus.all-tests
  (:require [cljs.nodejs :as nodejs]
            [clojure.test :refer-macros [run-tests]]
            [figwheel.client.utils :as fig.utils]

            [clojure-plus.helpers-test]
            [clojure-plus.modifications-test]))


(def vm (js/require "vm"))

(defn eval-helper [code {:keys [eval-fn] :as opts}]
    ((fn [src] (.runInThisContext vm src)) code))

(set! (-> js/figwheel .-client .-utils .-eval_helper) eval-helper)

(set! js/__dirname (str (.resolve (js/require "path") ".") "/lib/js/foo/bar"))
