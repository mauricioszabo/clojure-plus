(ns clj.__tracing__
  (:require [com.billpiel.sayid.core :as sayid]))

(defn reset-sayid! []
  (let [{:keys [fn ns]} (:traced (sayid/ws-get-active!))]
    (sayid/ws-reset!)
    (doseq [f fn] (sayid/ws-add-trace-fn!* f))
    (doseq [n ns] (sayid/ws-add-trace-ns!* n))))
