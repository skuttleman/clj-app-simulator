(ns com.ben-allred.app-simulator.templates.transformations.ws
  (:require
    [com.ben-allred.app-simulator.utils.strings :as strings]
    [com.ben-allred.app-simulator.templates.transformations.shared :as shared.tr]
    [com.ben-allred.formation.core :as f]))

(def source->model #(select-keys % #{:group :name :description}))

(def model->view
  {:path   str
   :method (comp #(subs % 1) str)})

(def view->model
  {:path   strings/trim-to-nil
   :method keyword})

(def model->source
  (f/make-transformer
    shared.tr/model->source))
