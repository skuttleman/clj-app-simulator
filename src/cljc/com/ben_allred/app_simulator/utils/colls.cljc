(ns com.ben-allred.app-simulator.utils.colls)

(defn force-sequential [v]
  (if (or (nil? v) (sequential? v))
    v
    [v]))

(defn replace-by [compare-fn value coll]
  (let [comparator (compare-fn value)]
    (cond->> coll
      :always (map #(if (= comparator (compare-fn %)) value %))
      (not (list? coll)) (into (empty coll)))))

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

(defn onto [coll]
  (fn [& vs]
    (into coll vs)))
