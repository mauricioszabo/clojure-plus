(ns clj.--check-deps--
  (:require [clojure.test :refer :all]))

(defn vars-in-form [form vars]
  (cond
    (coll? form) (reduce #(vars-in-form %2 %1) vars form)
    (symbol? form) (conj vars form)
    :else vars))

(defn vars-for-file [file-name]
  (let [code (load-string (str "'(" (slurp  file-name) "\n)"))
        filtered (filter #(not= 'ns (first %)) code)]
    (reduce (fn [vars list-of-forms]
              (vars-in-form (load-string (str "`" list-of-forms)) vars))
            #{} filtered)))

(defn unused-namespaces [file]
  (let [nss-in-file (->> file
                         vars-for-file
                         (map str)
                         (filter #(re-find #"/" %))
                         (map #(first (clojure.string/split % #"/")))
                         set)
        parsed-ns (refactor-nrepl.ns.ns-parser/parse-ns file)
        requires (map #(-> % :ns str) (concat (get-in parsed-ns [:clj :require]) (get-in parsed-ns [:cljs :require])))]
    (remove #(contains? nss-in-file %) requires)))

(defn fq-path-to-relative [fq-path]
  (let [root (System/getProperty "user.dir")
        size (count root)]
    (if (.startsWith fq-path root)
      (.substring fq-path (inc size))
      fq-path)))

(defn symbols-in-project []
  (let [rt (clojure.lang.RT/baseLoader)]
    (for [namespace (->> (all-ns))
          :let [ns-name (.name namespace)]
          symbol-map (ns-publics ns-name)
          :let [fq-symbol (.val symbol-map)
                meta-symbol (meta fq-symbol)
                file-path (:file meta-symbol)
                file-str (when file-path
                           (some-> rt
                                   (.getResource file-path)
                                   (.getFile)))]]
      (assoc meta-symbol :fqname (str ns-name "/" (.key symbol-map))
                         :fqpath file-str))))

(defn find-dependencies [symbols to-find]
  (let [search (fn search [sexp]
                 (if (coll? sexp)
                   (some search sexp)
                   (= sexp to-find)))
        have-sym? (fn [s]
                    (let [sym (-> s :fqname symbol)
                          source (try (clojure.repl/source-fn sym) (catch Exception _))
                          quoted (str "`" source)]
                      (in-ns (-> s (clojure.string/split #"/") first symbol))
                      (search (try (load-string quoted) (catch Exception e)))))]
    (filter have-sym? symbols)))

(defn decompress-all [temp-dir [jar-path partial-jar-path within-file-path]]
  (let [decompressed-path (str temp-dir "/" partial-jar-path)
        decompressed-file-path (str decompressed-path "/" within-file-path)
        decompressed-path-dir (clojure.java.io/file decompressed-path)]
    (when-not (.exists decompressed-path-dir)
      (println "decompressing" jar-path "to" decompressed-path)
      (.mkdirs decompressed-path-dir)
      (clojure.java.shell/sh "unzip" jar-path "-d" decompressed-path))
    decompressed-file-path))

(defn extract-jar-data [file-path]
  (re-find #"file:(.+/\.m2/repository/(.+\.jar))!/(.+)" file-path))

(def runtime (delay (clojure.lang.RT/baseLoader)))
(defn get-full-path [file]
  (some->> file (.getResource @runtime) .getPath))

(defn goto-var [var-sym temp-dir]
  (require 'clojure.repl)
  (require 'clojure.java.shell)
  (require 'clojure.java.io)
  (let [the-var (or (some->> (or (get (ns-aliases *ns*) var-sym) (find-ns var-sym))
                             clojure.repl/dir-fn
                             first
                             name
                             (str (name var-sym) "/")
                             symbol)
                    var-sym)
        {:keys [file line]} (meta (eval `(var ~the-var)))
        file-path (get-full-path file)]
    (if-let [[_ & jar-data] (extract-jar-data file-path)]
      [(decompress-all temp-dir jar-data) line]
      [file-path line])))

(defn symbols-from-ns [ns-ref]
  (for [sym-name (map first (ns-interns ns-ref))
        :let [ns-name (.name ns-ref)
              fq-str (str "#'" ns-name "/" sym-name)
              fqname (load-string fq-str)]]
    (merge {:ns ns-name :symbol sym-name} (select-keys (meta fqname) [:line :column]))))

(defn symbols-from-ns-in-json [ns-ref]
  (let [syms (symbols-from-ns ns-ref)
        jsons (map #(str "{\"line\":" (:line %)
                         ",\"column\":" (:column %)
                         ",\"symbol\":\"" (:symbol %) "\""
                         "}")
                   syms)]
    (str "[" (clojure.string/join "," jsons) "]")))

(defn resolve-missing [name]
  (let [edn-str (or (refactor-nrepl.ns.resolve-missing/resolve-missing {:symbol name}) "[]")
        namespace-names (->> edn-str read-string (map :name) (apply sorted-set))
        aliases (for [aliases (mapcat second (refactor-nrepl.ns.libspecs/namespace-aliases))
                      :let [[alias namespaces] aliases]
                      ns-name namespaces
                      :when (contains? namespace-names ns-name)]
                  [ns-name alias])
        alias-map (group-by first aliases)]
    (mapcat #(or (get alias-map %) [[% nil]]) namespace-names)))

(defn- normalize-clj-name [fn-name]
  (-> fn-name
    (clojure.string/replace #"_BANG_" "!")
    (clojure.string/replace #"_SLASH_" "/")
    (clojure.string/replace #"_AMPERSAND_" "&")
    (clojure.string/replace #"_STAR_" "*")
    (clojure.string/replace #"_PLUS_" "+")
    (clojure.string/replace #"_GT_" ">")
    (clojure.string/replace #"_GTE_" ">=")
    (clojure.string/replace #"_LT_" "<")
    (clojure.string/replace #"_LTE_" "<=")
    (clojure.string/replace #"(eval\d+\$|__\d+)" "")
    (clojure.string/replace #"_" "-")
    (clojure.string/replace #"_" "-")
    (clojure.string/split #"\$")))

;; Pretty stack traces
(defn- correct-file-name [stack-file-name file-from-meta raw-ns-name fn-name]
  (let [n (clojure.string/replace raw-ns-name #"\." "/")
        from-ns-name #(or (get-full-path (str n ".clj"))
                          (get-full-path (str n ".cljc"))
                          (get-full-path (str n ".cljx"))
                          (get-full-path (str n ".cljs"))
                          (get-full-path (str n ".cljr")))]
    (if (= fn-name "[anon-function]")
      (from-ns-name)
      (or (get-full-path file-from-meta) (from-ns-name)))))

(defn clj-trace [class-name stack-file-name line-number]
  (let [[raw-ns-name _] (clojure.string/split class-name #"\$")
        [ns-name fn-name] (normalize-clj-name class-name)
        fq-symbol (ns-resolve (symbol ns-name) (symbol fn-name))
        fn-name (cond
                  (re-matches #"eval\d+" fn-name) "[inline-eval]"
                  (re-matches #"^fn$" fn-name) "[anon-function]"
                  :else fn-name)
        file-from-meta (when-not (= fn-name "[anon-function]") (:file (meta fq-symbol)))
        file-to-open (correct-file-name stack-file-name file-from-meta raw-ns-name fn-name)]

    {:fn (str ns-name "/" fn-name)
     :file (or file-from-meta (some-> file-to-open (clojure.string/replace #".*!/?" "")))
     :line line-number
     :link file-to-open}))

(defn other-trace [stack-line]
  {:fn (str (.getClassName stack-line) "/" (.getMethodName stack-line))
   :file (.getFileName stack-line)
   :line (.getLineNumber stack-line)})

(defn prettify-stack [stack-line]
  "Given a stack-line, we'll try to return it in a legible way. You can use it with
this very simple code:

(try ..your-code-here..
  (catch Exception e (map prettify-stack (.getStackTrace e))))"
  (let [filename (.getFileName stack-line)
        clj-file? (re-find #"\.clj[cxs]?$" filename)]

    (if clj-file?
      (clj-trace (.getClassName stack-line) (.getFileName stack-line) (.getLineNumber stack-line))
      (other-trace stack-line))))

(defn to-thread [code]
  (let [f (fn f [vars acc]
            (if (not (seq? vars))
              (cons vars acc)
              (let [[fun first & rest] vars]
                (cond
                  (and first (empty? rest)) (f first (cons fun acc))
                  first (f first (cons (cons fun rest) acc))
                  :else (cons acc)))))]
    (cons '-> (f code nil))))

(def last-exception (atom nil))

(def watches (atom {}))
