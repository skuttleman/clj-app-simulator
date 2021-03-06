(ns com.ben-allred.app-simulator.utils.colls
  (:refer-clojure :exclude [assoc]))

(defn force-sequential [v]
  (if (or (nil? v) (sequential? v))
    v
    [v]))

(defn replace-by [compare-fn value coll]
  (let [comparator (compare-fn value)]
    (cond->> coll
      :always (map #(if (= comparator (compare-fn %)) value %))
      (not (list? coll)) (into (empty coll)))))

(defn assoc [coll id-fn value]
  (let [replaced? (volatile! false)
        comparator (id-fn value)]
    (cond-> coll
      :always (cond->>
                :always (mapv #(if (= comparator (id-fn %))
                                 (do (vreset! replaced? true)
                                     value)
                                 %))
                (and (sequential? coll) (not (vector? coll))) (seq)
                (not (sequential? coll)) (into (empty coll)))
      (not @replaced?) (conj value))))

(defn prepend
  ([x]
   (fn [rf]
     (let [prepended? (volatile! false)]
       (fn
         ([]
          (rf (rf) x))
         ([result]
          (if @prepended?
            (rf result)
            (rf (rf result x))))
         ([result input]
          (if @prepended?
            (rf result input)
            (do (vreset! prepended? true)
                (rf (rf result x) input))))))))
  ([coll x]
   (if (vector? coll)
     (into [x] coll)
     (cons x coll))))

(defn append
  ([x]
   (fn [rf]
     (fn
       ([]
        (rf))
       ([result]
        (rf (unreduced (rf result x))))
       ([result input]
        (rf result input)))))
  ([coll x]
   (if (vector? coll)
     (conj coll x)
     (concat coll [x]))))
