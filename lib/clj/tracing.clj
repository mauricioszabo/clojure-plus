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
                           :returned ret
                           :children nil})
                        (:let-binds child))
        maps (map (fn [[ret var _]] [var ret]) (:let-binds child))
        let-fn {:fn (-> child :inner-tags last name)
                :args (->> maps (mapv #(symbol (s/join " " %))) str vector)
                :mapping (into {} maps)
                :returned (:return child)
                :children (trace-child child)}]
    (conj outer-fns let-fn)))

(defn- function [child]
  [{:id (:id child)
    :fn (-> child :name str)
    :args (mapv truncate (:args child))
    :mapping (->> child :arg-map deref not-empty)
    :returned (:return child)
    :children (trace-child child)}])

(defn- trace-child [workspace]
  (when-let [children (some-> workspace :children deref not-empty)]
    (mapcat (fn [child]
              (condp #(get %2 %1) child
                :let-binds (let-binds child)
                (function child)))
            children)))

(defn trace-str []
  (trace-child (sayid/ws-get-active!)))

(defn trace-inner [id]
  (when-let [call (-> [:id id] sayid/ws-query :children first)]
    (let [f (-> call :name)
          args (:args call)]
      (sayid/ws-add-inner-trace-fn!* f)
      (apply (resolve f) args)
      (trace-child (some-> (sayid/ws-get-active!) :children deref last)))))

; (last foo)

; {:args [11 20],
;  :path [:root33447 :33563],
;  :children
;  #<Atom@6bb7c340:
;    [{:path [:root33447 :33563 :33564],
;      :children
;      #<Atom@404aba2c:
;        [{:path [:root33447 :33563 :33564 :33565],
;          :children
;          #<Atom@7ff6d273:
;            [{:args [31],
;              :path [:root33447 :33563 :33564 :33565 :33566],
;              :children #<Atom@7c4c9a48: []>,
;              :src-pos
;              {:line 9,
;               :column 18,
;               :end-line 9,
;               :end-column 25,
;               :file
;               "/home/mauricio/github/clojure-plus/lib/clj/tracing-test.clj"},
;              :return 32,
;              :started-at 1481405147141,
;              :ns clj.__tracing-test__,
;              :name inc,
;              :parent-path [:root33447 :33563 :33564 :33565],
;              :inner-path [2 :macro 1 1],
;              :parent-name clj.__tracing-test__/test1,
;              :arg-map #<Delay@2934eb42: :not-delivered>,
;              :xpanded-parent [temp__4657__auto__ (inc a)],
;              :id :33566,
;              :macro? nil,
;              :ended-at 1481405147141,
;              :throw nil,
;              :depth 4,
;              :form (inc a),
;              :inner-path-chain [[] [2] [2 :macro 1 1]],
;              :xpanded-frm (inc a)}
;             {:args [32 54],
;              :path [:root33447 :33563 :33564 :33565 :33567],
;              :children
;              #<Atom@3cbaaf92:
;                [{:path [:root33447 :33563 :33564 :33565 :33567 :33568],
;                  :children
;                  #<Atom@7596873c:
;                    [{:args [31 -9 32],
;                      :path
;                      [:root33447
;                       :33563
;                       :33564
;                       :33565
;                       :33567
;                       :33568
;                       :33569],
;                      :children #<Atom@6d90cd1c: []>,
;                      :src-pos
;                      {:line 10,
;                       :column 7,
;                       :end-line 10,
;                       :end-column 16,
;                       :file
;                       "/home/mauricio/github/clojure-plus/lib/clj/tracing-test.clj"},
;                      :return 54,
;                      :started-at 1481405147141,
;                      :ns clj.__tracing-test__,
;                      :name +,
;                      :parent-path
;                      [:root33447 :33563 :33564 :33565 :33567 :33568],
;                      :inner-path [2 :macro 2 :macro 2 1 :macro 2],
;                      :parent-name clj.__tracing-test__/test1,
;                      :arg-map #<Delay@63704517: :not-delivered>,
;                      :xpanded-parent
;                      (let* [c temp__4657__auto__] (+ a b c)),
;                      :id :33569,
;                      :macro? nil,
;                      :ended-at 1481405147141,
;                      :throw nil,
;                      :depth 6,
;                      :form (+ a b c),
;                      :inner-path-chain
;                      [[]
;                       [2]
;                       [2 :macro 2]
;                       [2 :macro 2 :macro 2 1]
;                       [2 :macro 2 :macro 2 1 :macro 2]],
;                      :xpanded-frm (+ a b c)}]>,
;                  :src-pos {},
;                  :return 54,
;                  :ns clj.__tracing-test__,
;                  :name clojure.core/let,
;                  :inner-tags [:macro :clojure.core/let],
;                  :parent-path [:root33447 :33563 :33564 :33565 :33567],
;                  :inner-path [2 :macro 2 :macro 2 1],
;                  :parent-name clj.__tracing-test__/test1,
;                  :xpanded-parent
;                  (do (let* [c temp__4657__auto__] (+ a b c))),
;                  :id :33568,
;                  :macro? true,
;                  :depth 5,
;                  :form
;                  (clojure.core/let [c temp__4657__auto__] (+ a b c)),
;                  :inner-path-chain
;                  [[] [2] [2 :macro 2] [2 :macro 2 :macro 2 1]],
;                  :xpanded-frm
;                  (let* [c temp__4657__auto__] (+ a b c))}]>,
;              :src-pos {},
;              :return 54,
;              :ns clj.__tracing-test__,
;              :name clojure.core/when,
;              :inner-tags [:macro :clojure.core/when],
;              :parent-path [:root33447 :33563 :33564 :33565],
;              :inner-path [2 :macro 2],
;              :parent-name clj.__tracing-test__/test1,
;              :xpanded-parent
;              (let*
;               [temp__4657__auto__ (inc a)]
;               (if
;                temp__4657__auto__
;                (do (let* [c temp__4657__auto__] (+ a b c))))),
;              :id :33567,
;              :macro? true,
;              :test-form 'temp__4657__auto__,
;              :depth 4,
;              :form
;              (clojure.core/when
;               temp__4657__auto__
;               (clojure.core/let [c temp__4657__auto__] (+ a b c))),
;              :inner-path-chain [[] [2] [2 :macro 2]],
;              :xpanded-frm
;              (if
;               temp__4657__auto__
;               (do (let* [c temp__4657__auto__] (+ a b c))))}]>,
;          :src-pos
;          {:line 9,
;           :column 5,
;           :end-line 10,
;           :end-column 17,
;           :file
;           "/home/mauricio/github/clojure-plus/lib/clj/tracing-test.clj"},
;          :return 54,
;          :ns clj.__tracing-test__,
;          :name when-let,
;          :inner-tags [:macro :when-let],
;          :parent-path [:root33447 :33563 :33564],
;          :inner-path [2],
;          :parent-name clj.__tracing-test__/test1,
;          :xpanded-parent
;          (let*
;           [a (+ x y) b (- x y)]
;           (let*
;            [temp__4657__auto__ (inc a)]
;            (if
;             temp__4657__auto__
;             (do (let* [c temp__4657__auto__] (+ a b c)))))),
;          :id :33565,
;          :macro? true,
;          :depth 3,
;          :form (when-let [c (inc a)] (+ a b c)),
;          :inner-path-chain [[] [2]],
;          :xpanded-frm
;          (let*
;           [temp__4657__auto__ (inc a)]
;           (if
;            temp__4657__auto__
;            (do (let* [c temp__4657__auto__] (+ a b c)))))}]>,
;      :src-pos
;      {:line 7,
;       :column 3,
;       :end-line 10,
;       :end-column 18,
;       :file
;       "/home/mauricio/github/clojure-plus/lib/clj/tracing-test.clj"},
;      :return 54,
;      :ns clj.__tracing-test__,
;      :name let*,
;      :let-binds [[31 a (+ x y)] [-9 b (- x y)]],
;      :inner-tags [:let],
;      :parent-path [:root33447 :33563],
;      :inner-path [],
;      :parent-name clj.__tracing-test__/test1,
;      :xpanded-parent nil,
;      :id :33564,
;      :macro? nil,
;      :depth 2,
;      :form
;      (let*
;       [a (+ x y) b (- x y)]
;       (let*
;        [temp__4657__auto__ (inc a)]
;        (if
;         temp__4657__auto__
;         (do (let* [c temp__4657__auto__] (+ a b c)))))),
;      :inner-path-chain [[]],
;      :xpanded-frm
;      (let*
;       [a (+ x y) b (- x y)]
;       (let*
;        [temp__4657__auto__ (inc a)]
;        (if
;         temp__4657__auto__
;         (do (let* [c temp__4657__auto__] (+ a b c))))))}]>,
;  :meta
;  {:arglists ([x y]),
;   :line 6,
;   :column 6,
;   :file "/home/mauricio/github/clojure-plus/lib/clj/tracing-test.clj",
;   :name test1,
;   :ns
;   #object[clojure.lang.Namespace 0x2418517a "clj.__tracing-test__"]},
;  :return 54,
;  :started-at 1481405147141,
;  :name clj.__tracing-test__/test1,
;  :arg-map #<Delay@a5fb0d4: :not-delivered>,
;  :id :33563,
;  :ended-at 1481405147141,
;  :depth 1}



(sayid/ws-print)
