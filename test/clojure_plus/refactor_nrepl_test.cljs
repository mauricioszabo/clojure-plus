(ns clojure-plus.refactor-nrepl-test
  (:require [clojure.test :refer-macros [deftest is testing run-tests async]]
            [clojure-plus.test-helpers :as th]
            [clojure-plus.refactor-nrepl :as refactor]))

(deftest ns-data
  (testing "rewrites NS"
      (th/with-text-editor editor
        (th/set-text editor "(ns something
  (:require
            [clojure.walk :as walk]))
:foo")
        (refactor/rewrite-ns editor "BAR")
        (is (= "BAR\n:foo") (th/text editor))))

  (async done
    (testing "removes unused NS imports"
      (th/with-text-editor editor
        (th/set-text editor "(ns something
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [clojure.walk :as walk]))
(str/join (walk/prewalk [1 2 3] str))")
        (refactor/organize-ns editor)
        (is (= "(ns something
  (:require [clojure.string :as str]
            [clojure.walk :as walk]))
(str/join (walk/prewalk [1 2 3] str))"
               (th/text editor)))
        (done)))))

(run-tests)
