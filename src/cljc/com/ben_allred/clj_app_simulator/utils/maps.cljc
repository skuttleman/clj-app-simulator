(ns com.ben-allred.clj-app-simulator.utils.maps)

(defn update-maybe [m k f & f-args]
    (if (get m k)
        (apply update m k f f-args)
        m))

(defn map-kv [key-fn val-fn m]
    (->> m
        (map (fn [[k v]] [(key-fn k) (val-fn v)]))
        (into {})))

(defn map-keys [key-fn m]
    (map-kv key-fn identity m))

(defn map-vals [val-fn m]
    (map-kv identity val-fn m))

(defn update-all [m f & f-args]
    (map-vals #(apply f % f-args) m))

(defmacro ->map [& vars]
    (loop [m {} [v1 v2 :as vs] vars]
        (cond
            (empty? vs) m
            (symbol? v1) (recur (assoc m (keyword v1) v1) (next vs))
            :else (recur (assoc m v1 v2) (nnext vs)))))
