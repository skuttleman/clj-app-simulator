(ns test.utils.spies)

(defn spy-on [f]
    (let [calls (atom [])]
        (with-meta
            (fn [& args]
                (swap! calls conj args)
                (apply f args))
            {:calls calls})))

(defn create-spy []
    (spy-on (constantly nil)))

(defn get-calls [spy]
    (when-let [calls (:calls (meta spy))]
        @calls))

(defn called-with? [spy & args]
    (some (partial = args) (get-calls spy)))

(defn called-times? [spy n]
    (= n (count (get-calls spy))))

(defn called-with-times? [spy n & args]
    (->> spy
        (get-calls)
        (filter (partial = args))
        (count)
        (= n)))

(defn never-called? [spy]
    (empty? (get-calls spy)))

(def called? (complement never-called?))

(defn reset-spy! [spy]
    (when-let [calls (:calls (meta spy))]
        (reset! calls [])))
