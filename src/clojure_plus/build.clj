(ns clojure-plus.build
  (:require [cljs.repl :as repl]
            [cljs.build.api :as api]
            [cljs.repl.node :as node]))


(defn repl []
  (api/build "src"
             {;:id "dev"
              :main 'clojure-plus.core
              :output-to "lib/js/main.js"
              :target :nodejs
              :optimizations :simple
              :output-wrapper true
              :pretty-print true})
              ; :output-dir "lib/js"})
  ;  :verbose true})

  (repl/repl (node/repl-env)
             :watch "src"
             :output-dir "lib/js"))
