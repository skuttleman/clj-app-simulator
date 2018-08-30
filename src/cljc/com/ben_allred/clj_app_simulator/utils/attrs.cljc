(ns com.ben-allred.clj-app-simulator.utils.attrs
  (:require [clojure.string :as string]
            [com.ben-allred.clj-app-simulator.utils.strings :as strings]))

(defn classes
  ([rules] (classes nil rules))
  ([attrs rules]
   (let [classes (->> rules
                      (filter val)
                      (map (comp name key))
                      (string/join " "))]
     (cond-> attrs
       (seq classes) (update :class-name (comp strings/trim-to-nil str) " " classes)))))
