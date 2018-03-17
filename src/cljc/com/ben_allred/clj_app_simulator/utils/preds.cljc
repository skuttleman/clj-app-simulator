(ns com.ben-allred.clj-app-simulator.utils.preds
    #?(:cljs (:refer-clojure :exclude [regexp?])
       :clj (:import [java.util.regex Pattern])))

(defn or? [& preds]
    (fn [value]
        (reduce #(or %1 (%2 value)) false preds)))

(defn regexp? [value]
    #?(:clj  (instance? Pattern value)
       :cljs (cljs.core/regexp? value)))
