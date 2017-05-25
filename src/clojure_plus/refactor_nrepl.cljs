(ns clojure-plus.refactor-nrepl
  (:require [clojure-plus.repl :as repl]
            [cljs.reader :as edn]
            [clojure.string :as str]))

(def ^:private tmp (.tmpdir (js/require "os")))
(def ^:private path (js/require "path"))
(def ^:private fs (js/require "fs"))

(defn- ns-range [editor]
  (let [top-levels (-> js/protoRepl .-EditorUtils (.getTopLevelRanges editor))]
    (first (filter #(re-find #"\(\s*ns" (.getTextInBufferRange editor %)) top-levels))))

(defn rewrite-ns [editor text]
  (let [range (ns-range editor)]
    (.setTextInBufferRange editor range text)))

(defn- format-require [[key & rest]]
  ; 2 spaces, 1 parenthesis, and one space
  (let [indent (->> (+ 4 (count (str key)))
                    range
                    (map (constantly " "))
                    str/join
                    (str "\n"))]
    (str "  (" key " " (str/join indent rest) ")")))

(defn- format-ns [[_ ns-name & requires]]
  (str "(ns " ns-name "\n"
       (->> requires (map format-require) str/join)
       ")"))

(defn organize-ns [editor]
  (let [temp-file (.join path tmp (str "tmp_" (gensym) ".clj"))
        cmd `(~'clojure.core/with-bindings
               {#'refactor-nrepl.config/*config* {:debug false,
                                                  :prune-ns-form true}}
               (refactor-nrepl.ns.clean-ns/clean-ns {:path ~temp-file}))]
    (.writeFileSync fs temp-file (.getText editor))
    (repl/execute-cmd cmd "user" (fn [res]
                                   (println "RESULT" res)
                                   (when-let [ns-res (:value res)]
                                       (try
                                         (rewrite-ns editor (format-ns ns-res))
                                         (finally
                                           (.unlink fs temp-file))))))))

(defn add-require [editor require-str]
  (let [range (ns-range editor)
        ns-txt (.getTextInBufferRange editor range)
        new-ns (if (re-find #"\(\s*:?require" ns-txt)
                 (str/replace-first ns-txt
                                    #"\(\s*:?require"
                                    (str "(:require " require-str))
                 (str/replace ns-txt #"\)" (str "(:require " require-str "))")))]
    (->> new-ns edn/read-string format-ns (rewrite-ns editor))))
