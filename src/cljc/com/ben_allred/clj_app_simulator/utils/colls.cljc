(ns com.ben-allred.clj-app-simulator.utils.colls)

(defn force-sequential [v]
  (if (or (nil? v) (sequential? v))
    v
    [v]))

(defn replace-by [compare-fn value coll]
  (let [comparator (compare-fn value)]
    (cond->> coll
      :always (map #(if (= comparator (compare-fn %)) value %))
      (not (list? coll)) (into (empty coll)))))
