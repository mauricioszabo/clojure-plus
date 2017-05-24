(ns clojure-plus.test-helpers)

(def ^:private tmp (.tmpdir (js/require "os")))
(def ^:private path (js/require "path"))
(def ^:private fs (js/require "fs"))

(defn with-text-editor* [f]
  (-> js/atom .-workspace (.open (str "test_" (gensym) ".clj"))
      (.then (fn [editor]
               (f editor)
               (.destroy editor)))))

(def set-text #(.setText %1 %2))
(def text #(.getText %))
