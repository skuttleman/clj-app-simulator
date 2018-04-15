(ns com.ben-allred.clj-app-simulator.utils.core)

(defmacro => [& forms]
  `(fn [value#]
     (-> value# ~@forms)))
