(defproject clojure-plus "0.0.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.456"]
                ;  [figwheel-sidecar "0.5.9"]
                 [com.cemerick/piggieback "0.2.1"]
                 [weasel "0.7.0" :exclusions [org.clojure/clojurescript]]]

  :plugins [[lein-cljsbuild "1.1.5"]
            [lein-figwheel "0.5.9"]]

            ; [org.clojure/tools.nrepl "0.2.10"]]

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :source-paths ["lib" "src"]
  :clean-targets ^{:protect false} ["lib/js"]
  :cljsbuild {:builds [{:id "server-dev"
                        :source-paths ["src"]
                        ; :figwheel true
                        :compiler {;:main clojure-plus.core
                                   :output-to "lib/js/main.js"
                                   :output-dir "lib/js"
                                  ;  :target :nodejs
                                   :pretty-print true
                                   :optimizations :simple}}]})
                                  ;  :source-map true}}
                      ;  {:id "figwheel-dev"
                      ;   :source-paths ["src2"]
                      ;   :figwheel true
                      ;   :compiler {;:main clojure-plus.core
                      ;              ;:main clojure-plus.initial
                      ;              :output-to "lib/js2/main.js"
                      ;              :output-dir "lib/js2"
                      ;              :target :nodejs
                      ;              :optimizations :none
                      ;              :source-map true}}]}
  ; :figwheel {})