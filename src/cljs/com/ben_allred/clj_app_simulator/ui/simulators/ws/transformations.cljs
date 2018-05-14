(ns com.ben-allred.clj-app-simulator.ui.simulators.ws.transformations
  (:require [com.ben-allred.formation.core :as f]
            [com.ben-allred.clj-app-simulator.utils.strings :as strings]))

(def source->model identity)

(def model->view
  {:path     str
   :method   (comp #(subs % 1) str)})

(def view->model
  {:path     strings/trim-to-nil
   :method   keyword})

(def model->source
  (f/make-transformer
    {:name        strings/trim-to-nil
     :group       strings/trim-to-nil
     :description strings/trim-to-nil}))

(defn sim->model [sim]
  (-> sim
      (:config)
      (select-keys #{:group :name :description})
      (source->model)))
