(ns com.ben-allred.app-simulator.utils.sets
  #?(:clj
     (:import
       (clojure.lang IFn IPersistentSet))))

(declare ordered*)

(defn ^:private -disj [this set vec value]
  (if (contains? set value)
    (let [next (disj set value)]
      (with-meta (ordered* next
                           (into [] (filter (partial contains? next)) vec))
                 (meta this)))
    this))

(defn ^:private -cons [this set vec value]
  (if (contains? set value)
    this
    (with-meta (ordered* (conj set value) (conj vec value))
               (meta this))))

(defn ^:private -= [set vec other]
  (if (set? other)
    (= set other)
    (= vec other)))

(defn ^:private ordered* [set vec]
  #?(:clj  (reify
             IPersistentSet
             (disjoin [this value] (-disj this set vec value))
             (cons [this value] (-cons this set vec value))
             (contains [_ value] (contains? set value))
             (get [_ value] (get set value))
             (empty [this] (with-meta (ordered* #{} []) (meta this)))
             (count [_] (count set))
             (equiv [_ other] (-= set vec other))
             (seq [_] (seq vec))
             IFn
             (invoke [_ value] (set value)))
     :cljs (reify
             ISet
             (-disjoin [this value] (-disj this set vec value))
             ICollection
             (-conj [this value] (-cons this set vec value))
             ILookup
             (-lookup [_ value] (get set value))
             (-lookup [_ value default] (get set value default))
             IEmptyableCollection
             (-empty [this] (with-meta (ordered* #{} []) (meta this)))
             ICounted
             (-count [_] (count set))
             IEquiv
             (-equiv [_ other] (-= set vec other))
             ISeqable
             (-seq [_] (seq vec))
             IFn
             (-invoke [_ value] (set value)))))

(def ordered
  "A set that retains insertion order. Disjoin happens in linear time."
  (ordered* #{} []))
