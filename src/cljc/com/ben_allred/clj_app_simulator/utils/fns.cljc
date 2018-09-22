(ns com.ben-allred.clj-app-simulator.utils.fns
  (:refer-clojure :exclude [and or]))

(defmacro => [& forms]
  `(fn [arg#]
     (-> arg# ~@forms)))

(defmacro =>> [& forms]
  `(fn [arg#]
     (->> arg# ~@forms)))

(defn and [& values]
  (loop [[val & more] values]
    (if (empty? more)
      val
      (clojure.core/and val (recur more)))))

(defn or [& values]
  (loop [[val & more] values]
    (if (empty? more)
      val
      (clojure.core/or val (recur more)))))
