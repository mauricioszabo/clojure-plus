(ns ^:figwheel-always clojure-plus.all-tests
  (:require [clojure.test :refer-macros [run-tests]]
            [figwheel.client.utils :as fig.utils]
            [clojure-plus.helpers-test]
            [clojure-plus.modifications-test]
            [clojure-plus.repl-test]))

(def vm (js/require "vm"))

(defn eval-helper [code {:keys [eval-fn] :as opts}]
    ((fn [src] (.runInThisContext vm src)) code))

(set! (-> js/figwheel .-client .-utils .-eval_helper) eval-helper)

(set! js/__dirname (str (-> js/atom .-packages .getPackageDirPaths)
                        "/clojure-plus/lib/js/foo/bar"))

(js/setTimeout #(do
                  (println "BAR" (.-exports js/module))
                  (set! (.-exports js/module)
                    #js {:eval_str "FOO"})))
               ;
               ; js/atom
               ; (js/require "atom")
               ;
               ; atom.packages.getActivePackage('clojure-plus').path
