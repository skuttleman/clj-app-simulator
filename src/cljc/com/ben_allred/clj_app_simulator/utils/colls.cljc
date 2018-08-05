(ns com.ben-allred.clj-app-simulator.utils.colls)

(defn force-sequential [v]
  (if (or (nil? v) (sequential? v))
    v
    [v]))
