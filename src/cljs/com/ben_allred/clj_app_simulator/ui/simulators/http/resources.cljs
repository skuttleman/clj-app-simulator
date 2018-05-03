(ns com.ben-allred.clj-app-simulator.ui.simulators.http.resources
  (:require [com.ben-allred.clj-app-simulator.utils.fns :as fns :include-macros true]
            [com.ben-allred.formation.core :as f]
            [com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]
            [clojure.string :as string]
            [com.ben-allred.clj-app-simulator.utils.strings :as strings]
            [com.ben-allred.clj-app-simulator.services.http :as http]))

(def statuses
  (->> http/kw->status
       (map (juxt second #(str (strings/titlize (name (first %)) " ") " [" (second %) "]")))
       (sort-by first)))

(defn validate* []
  (f/make-validator
    {:delay    [(f/pred #(and (not (js/isNaN %)) (number? %)) "Delay must be a number")
                (f/pred #(= (js/parseInt %) %) "Delay must be a whole number")
                (f/pred #(>= % 0) "Delay cannot be negative")]
     :response {:status  (f/required "Must have a status")
                :headers (f/validator-coll
                           (f/validator-tuple
                             (f/pred (comp (partial re-matches #"[A-Za-z0-9_-]+") name) "Invalid header key")
                             (f/pred #(seq %) "Header must have a value")))}}))

(def validate (validate*))
