(ns clojure-plus.core
  (:require ;[clojure.browser.repl :as repl]
            [cljs.nodejs :as nodejs]
            [weasel.repl :as repl]))
;             [clojure.browser.repl :as repl]))
;

; (def vm (js/require "vm"))
; (defn eval-code [code]
;   ())

(+ 1 2)

(def vm (js/require "vm"))

(defmethod repl/process-message :eval-js [message]
  (let [code (:code message)]
    {:op :result
     :value (try
              ; {:status :success, :value (str (js* "eval(~{code})"))}
              {:status :success, :value (str ((fn [src] (.runInThisContext vm src)) code))}
              (catch js/Error e
                {:status :exception
                 :value (pr-str e)
                 :stacktrace (if (.hasOwnProperty e "stack")
                               (.-stack e)
                               "No stacktrace available.")})
              (catch :default e
                {:status :exception
                 :value (pr-str e)
                 :stacktrace "No stacktrace available."}))}))


(js/setInterval #(when-not (repl/alive?)
                   (repl/connect "ws://localhost:9001"))
                3000)

(nodejs/enable-util-print!)
(.log js/console "WOW!!!!!")

; (println "Hello from the Node!")

; (.log js/console
;       "WOW DOIS!!!!!"
;       (repl/connect "http://localhost:9000/repl"))
;
; (repl/connect "http://localhost:9000/repl")
; (def -main (fn [] nil))
; (set! *main-cli-fn* -main) ;; this is required

; (ns clojure-plus.core
;    (:require [clojure.browser.repl :as repl]))
; ; Use of "localhost" will only work for local development.
; ; Change the port to match the :repl-listen-port.
; (repl/connect "http://localhost:9000/repl")
