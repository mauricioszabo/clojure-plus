(ns clojure-plus.core
  (:require [cljs.nodejs :as nodejs]
            [figwheel.client.utils :as fig.utils]
            [cljs.reader :as edn]))
            ; [weasel.repl :as repl]))

(defonce disposable
  (-> js/window (aget "clojure plus extensions") .-disposable))

; (set! (.-exports js/module)
;   #js {:foo "BAR"})
; (set! (-> js/module .-exports)
;       #js {:disposable disposable})
;
(nodejs/enable-util-print!)

(defn command-for [name f]
  (let [disp (-> js/atom .-commands (.add "atom-text-editor"
                                          (str "clojure-plus:" name)
                                          f))]
    (.add disposable disp)))

(def repl (-> (js/require "../../clojure-plus")
              .getCommands .-promisedRepl))

(defn execute-cmd
  ([cmd callback] (execute-cmd cmd {} callback))
  ([cmd ns-or-opts callback]
   (-> repl
       (.syncRun (str cmd) ns-or-opts)
       (.then #(if-let [val (.-value %)]
                 (callback {:value (edn/read-string val)})
                 (callback {:error (or (.-error %) %)}))))))
