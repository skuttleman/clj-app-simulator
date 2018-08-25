(ns com.ben-allred.clj-app-simulator.utils.fns)

(defmacro => [& forms]
  `(fn [arg#]
     (-> arg# ~@forms)))

(defmacro =>> [& forms]
  `(fn [arg#]
     (->> arg# ~@forms)))

(defn compare-by [& fns]
  (fn [item-1 item-2]
    (loop [result 0 [[idx f] :as fns] (map-indexed vector fns)]
      (if (or (empty? fns) (not (zero? result)))
        result
        (recur ((comparator f) (nth item-1 idx nil) (nth item-2 idx nil)) (rest fns))))))
