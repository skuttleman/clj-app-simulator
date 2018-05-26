(ns test.utils.spies
  (:refer-clojure :exclude [reset! constantly]))

(defn ^:private find-override [overrides args]
  (->> overrides
       (filter (comp #(% args) first))
       (map second)
       (first)))

(defn matcher? [obj]
  (::matcher? (meta obj)))

(defn ^:private match [matcher val]
  (when (matcher? matcher)
    (let [match (::match (meta matcher))]
      (match val))))

(defn ^:private matches* [val-1 val-2]
  (cond
    (matcher? val-1) (match val-1 val-2)
    (matcher? val-2) (match val-2 val-1)
    :else (= val-1 val-2)))

(defn ^:private matches? [args-1 args-2]
  (and (= (count args-1) (count args-2))
       (->> args-2
            (map matches* args-1)
            (every? boolean))))

(defn matcher [f]
  (with-meta
    f
    {::matcher? true
     ::match    f}))

(def any
  (matcher (clojure.core/constantly true)))

(defn spy? [obj]
  (::spy? (meta obj)))

(defn create
  ([]
   (create (clojure.core/constantly nil)))
  ([f]
   (let [calls (atom [])
         overrides (atom ())]
     (with-meta
       (fn [& args]
         (swap! calls conj args)
         (if-let [override (find-override @overrides args)]
           (apply override args)
           (apply f args)))
       {::spy?      true
        ::calls     calls
        ::overrides overrides}))))

(defn constantly [value]
  (create (clojure.core/constantly value)))

(defn and-then [& responses]
  (let [responses (atom responses)]
    (create (fn [& _]
              (let [response (first @responses)]
                (swap! responses #(if (= 1 (count %)) % (rest %)))
                response)))))

(defn calls [spy]
  (when (spy? spy)
    @(::calls (meta spy))))

(defn called-with? [spy & args]
  (some (partial matches? args) (calls spy)))

(defn called-times? [spy n]
  (= n (count (calls spy))))

(defn called-with-times? [spy n & args]
  (->> spy
       (calls)
       (filter (partial matches? args))
       (count)
       (= n)))

(defn never-called? [spy]
  (empty? (calls spy)))

(def called? (complement never-called?))

(defn reset! [& spies]
  (doseq [spy spies
          :let [{:keys [::calls ::overrides]} (meta spy)]
          :when (spy? spy)]
    (clojure.core/reset! calls [])
    (clojure.core/reset! overrides ())))

(defn do-when-called-with! [spy matcher f]
  (swap! (::overrides (meta spy)) conj [matcher f]))

(defn respond-with! [spy f]
  (do-when-called-with! spy (clojure.core/constantly true) f))

(defn returning! [spy & values]
  (let [values (atom values)]
    (do-when-called-with! spy
                          (clojure.core/constantly true)
                          (fn [& _]
                            (let [response (first @values)]
                              (swap! values rest)
                              response)))))
