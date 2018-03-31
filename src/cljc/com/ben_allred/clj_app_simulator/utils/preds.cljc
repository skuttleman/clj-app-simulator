(ns com.ben-allred.clj-app-simulator.utils.preds
  #?(:cljs (:refer-clojure :exclude [regexp?])
     :clj  (:import [java.util.regex Pattern])))

(defn regexp? [value]
  #?(:clj  (instance? Pattern value)
     :cljs (cljs.core/regexp? value)))
