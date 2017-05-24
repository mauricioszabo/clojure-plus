(ns clojure-plus.refactor-nrepl
  (:require [clojure-plus.repl :as repl]
            [clojure.string :as str]))

(def ^:private tmp (.tmpdir (js/require "os")))
(def ^:private path (js/require "path"))
(def ^:private fs (js/require "fs"))

(defn rewrite-ns [editor text])

(defn- format-require [[key & rest]]
  ; 2 spaces, 1 parenthesis, and one space
  (let [indent (->> (+ 4 (count (str key)))
                    range
                    (map (constantly " "))
                    str/join
                    (str "\n"))]
    (str "  (" key " " (str/join indent rest) ")")))

(defn organize-ns [editor]
  (let [temp-file (.join path tmp (str "tmp_" (gensym) ".clj"))
        cmd `(~'clojure.core/with-bindings
               {#'refactor-nrepl.config/*config* {:debug false,
                                                  :prune-ns-form true}}
               (refactor-nrepl.ns.clean-ns/clean-ns {:path ~temp-file}))]
    (.writeFileSync fs temp-file (.getText editor))
    (repl/execute-cmd cmd "user" (fn [res]
                                   (let [[_ ns-name & requires] (:value res)]
                                     (str "(ns " ns-name "\n"
                                          (->> requires (map format-require) str/join)
                                          ")"))
                                   (.unlink fs temp-file)))))
;
; (println (str "(ns " (second a) "\n"
;               (->> (drop 2 a)
;                    (map format-require)
;                    (str/join))
;               ")"))
