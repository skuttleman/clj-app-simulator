(ns com.ben-allred.clj-app-simulator.utils.colls
    (:refer-clojure :exclude [assoc assoc-in get get-in update update-in]))

(defn get [value k & [default]]
    (if (or (list? value) (seq? value))
        (nth value k default)
        (clojure.core/get value k default)))

(defn get-in [value [k & more :as ks] & [default]]
    (cond
        (empty? ks) value
        (seq more) (get-in (get value k) more default)
        :else (get value k default)))

(defn assoc [coll & {:as kvs}]
    (if (or (list? coll) (seq? coll))
        (map-indexed kvs coll)
        (apply clojure.core/assoc coll (mapcat vec kvs))))

(defn assoc-in [value [k & more :as ks] v]
    (cond
        (empty? ks) value
        (seq more) (assoc value k (assoc-in (get value k) more v))
        :else (assoc value k v)))

(defn update [value key f & f-args]
    (if (or (list? value) (seq? value))
        (do (assert (integer? key))
            (map-indexed #(if (= %1 key) (apply f %2 f-args) %2) value))
        (apply clojure.core/update value key f f-args)))

(defn update-in [value [k & more :as ks] f & f-args]
    (cond
        (empty? ks) value
        (seq more) (assoc value k (apply update-in (get value k) more f f-args))
        :else (apply update value k f f-args)))

(defn swap [coll idx-1 idx-2]
    (if (vector? coll)
        (assoc coll
            idx-1 (get coll idx-2)
            idx-2 (get coll idx-1))
        (->> coll
            (map-indexed #(cond
                              (= %1 idx-1) (nth coll idx-2)
                              (= %1 idx-2) (nth coll idx-1)
                              :else %2)))))

(defn exclude [coll idx]
    (cond-> coll
        :always (->>
                    (map-indexed vector)
                    (remove (comp (partial = idx) first))
                    (map second))
        (vector? coll) (vec)))

(defn prepend [coll value]
    (if (vector? coll)
        (into [value] coll)
        (conj coll value)))

(defn append [coll value]
    (if (or (list? coll) (seq? coll))
        (concat coll [value])
        (conj coll value)))
