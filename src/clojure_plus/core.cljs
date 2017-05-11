(ns clojure-plus.core
  (:require [cljs.nodejs :as nodejs]))

(defonce disposable
  (-> js/window (aget "clojure plus extensions") .-disposable))

(nodejs/enable-util-print!)

(defn command-for [name f]
  (let [disp (-> js/atom .-commands (.add "atom-text-editor"
                                          (str "clojure-plus:" name)
                                          f))]
    (.add disposable disp)))
