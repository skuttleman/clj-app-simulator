(ns com.ben-allred.clj-app-simulator.utils.colls)

(defn force-sequential [v]
  (if (or (nil? v) (sequential? v))
    v
    [v]))

(defn only [coll]
  (assert (>= 1 (count coll)) "Expected a singleton collection")
  (first coll))
