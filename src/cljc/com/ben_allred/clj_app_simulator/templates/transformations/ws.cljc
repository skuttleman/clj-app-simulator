(ns com.ben-allred.clj-app-simulator.templates.transformations.ws
  (:require [com.ben-allred.formation.core :as f]
            [com.ben-allred.clj-app-simulator.utils.strings :as strings]
            [com.ben-allred.clj-app-simulator.templates.transformations.shared :as shared.tr]))

(def source->model identity)

(def model->view
  {:path     str
   :method   (comp #(subs % 1) str)})

(def view->model
  {:path     strings/trim-to-nil
   :method   keyword})

(def model->source
  (f/make-transformer
    shared.tr/model->source))

(defn sim->model [sim]
  (-> sim
      (:config)
      (select-keys #{:group :name :description})
      (source->model)))

