(defproject clojure-plus "0.0.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.456"]
                 [com.cemerick/piggieback "0.2.1"]
                 [weasel "0.7.0" :exclusions [org.clojure/clojurescript]]
                 [expectations "2.1.0"]]

  :plugins [[lein-cljsbuild "1.1.5"]]

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :profiles {:dev {:dependencies [[midje "1.8.3"]]
                   :plugins [[lein-midje "3.2.1"]]}}

  :source-paths ["lib" "src"]
  :clean-targets ^{:protect false} ["lib/js"]
  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src" "test"]
                        :compiler {
                                   :main clojure-plus.helpers-test
                                   :output-to "lib/js/main.js"
                                   :target :nodejs
                                   :optimizations :simple
                                   :output-wrapper true
                                   :pretty-print true}}]})
