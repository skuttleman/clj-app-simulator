(ns com.ben-allred.clj-app-simulator.utils.fns)

(defmacro => [& forms]
  `(fn [arg#]
     (-> arg# ~@forms)))

(defmacro =>> [& forms]
  `(fn [arg#]
     (->> arg# ~@forms)))

(defn orf [& args]
  (->> args
       (filter identity)
       (first)))

(defn andf [& args]
  (->> args
       (take-while identity)
       (last)))
