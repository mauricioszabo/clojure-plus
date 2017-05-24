(ns clojure-plus.test-helpers)

(defmacro with-text-editor [editor-var & forms]
  `(with-text-editor* (fn [~editor-var] ~@forms)))
