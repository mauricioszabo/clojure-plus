(ns clj.__tracing__
  (:require [com.billpiel.sayid.core :as sayid]
            [clojure.string :as s]))

(defn reset-sayid! []
  (let [{:keys [fn ns]} (:traced (sayid/ws-get-active!))]
    (reset! sayid/workspace nil) ; Sayid bug?
    (with-out-str (sayid/ws-reset!))
    (doseq [f fn] (sayid/ws-add-trace-fn!* f))
    (doseq [n ns] (sayid/ws-add-trace-ns!* n))))

(defn- truncate [obj]
  (let [s (str obj)]
    (if (< (count s) 20)
      s
      (-> s (subs 0 20) (str "...")))))

(declare trace-child)
(defn- let-binds [child]
  (let [outer-fns (mapv (fn [[ret _ form]]
                          {:fn (str form)
                           :returned (str ret)
                           :children nil})
                       (:let-binds child))
        maps (map (fn [[ret var _]] [var (str ret)]) (:let-binds child))
        let-fn {:id (name (:id child))
                :fn (-> child :inner-tags last name)
                ; :args (->> maps (mapv #(symbol (s/join " " %))) str vector)
                :args (-> child :xpanded-frm second str vector)
                :mapping (into {} maps)
                :returned (str (:return child))
                :children (-> outer-fns (concat (trace-child child)) vec)}]
    [let-fn]))

(defn- function [child]
  (println child)
  [{:id (name (:id child))
    :fn (str (or #_(:form child) (:name child)))
    :args (not-empty (mapv truncate (:args child)))
    :mapping (some->> child :arg-map deref not-empty (map (fn [[k v]] [k (str v)])) (into {}))
    :returned (str (:return child))
    :children (trace-child child)}])

(defn- trace-child [workspace]
  (when-let [children (some-> workspace :children deref not-empty)]
    (mapcat (fn [child]
              (cond
                (:let-binds child) (let-binds child)
                (and (:form child) (-> child :src-pos empty?)) (trace-child child)
                :else (function child)))
            children)))

(defn trace-str []
  (trace-child (sayid/ws-get-active!)))

(defn trace-inner [id]
  (when-let [call (-> [:id id] sayid/ws-query :children first)]
    (clojure.pprint/pprint call)
    (let [var (ns-resolve (or (:ns call) *ns*) (:name call))
          f (symbol (str (.name (.ns var)) "/" (.sym var)))
          args (:args call)]
      (println var)
      (println f)
      (sayid/ws-add-inner-trace-fn!* f)
      (apply (resolve f) args)
      (trace-child (some-> (sayid/ws-get-active!) :children deref last)))))
