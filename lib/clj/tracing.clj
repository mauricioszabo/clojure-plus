(ns clj.__tracing__
  (:require [com.billpiel.sayid.core :as sayid]))

(defn reset-sayid! []
  (let [{:keys [fn ns]} (:traced (sayid/ws-get-active!))]
    (with-out-str (sayid/ws-reset!))
    (doseq [f fn] (sayid/ws-add-trace-fn!* f))
    (doseq [n ns] (sayid/ws-add-trace-ns!* n))))

(defn- truncate [obj]
  (let [s (str obj)]
    (if (< (count s) 20)
      s
      (-> s (subs 0 20) (str "...")))))

(defn- trace-child [children]
  (when (not-empty children)
    (for [child children]
      {:id (:id child)
       :fn (-> child :name str)
       :args (mapv truncate (:args child))
       :mapping @(:arg-map child)
       :returned (:return child)
       :children (trace-child @(:children child))})))

(defn trace-str
  ([] (trace-child (some-> (sayid/ws-get-active!) :children deref))))

; (trace-str)
