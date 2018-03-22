(ns com.ben-allred.clj-app-simulator.utils.strings
    (:require [clojure.string :as string]))

(defn trim-to-nil [s]
    (when-let [s (and s (string/trim s))]
        (when-not (empty? s)
            s)))

(defn maybe-pr-str [s]
    (cond-> s
        (not (string? s)) (pr-str)))