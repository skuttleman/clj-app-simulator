(ns com.ben-allred.clj-app-simulator.ui.simulators.ws.resources
  (:require [com.ben-allred.formation.core :as f]))

(defn validate-new* []
  (f/make-validator
    {:path   [(f/required "Must have a path")
              (f/pred #(or (= % "/") (re-matches #"(/:?[A-Za-z-_0-9]+)+" %)) "Invalid path")]
     :method (f/required "Must have a method")}))

(def validate-new (validate-new*))
