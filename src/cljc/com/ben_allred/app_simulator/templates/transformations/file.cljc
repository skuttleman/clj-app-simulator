(ns com.ben-allred.app-simulator.templates.transformations.file
  (:require
    [com.ben-allred.app-simulator.templates.transformations.shared :as shared.tr]
    [com.ben-allred.app-simulator.utils.uuids :as uuids]
    [com.ben-allred.formation.core :as f]))

(def source->model
  (f/make-transformer shared.tr/source->model*))

(def model->view
  (merge-with merge
              shared.tr/model->view
              {:response {:file str}}))

(def view->model
  (merge-with merge
              shared.tr/view->model
              {:response {:file uuids/->uuid}}))

(def model->source
  (f/make-transformer
    [{:response shared.tr/headers->source}
     shared.tr/model->source]))

(defn sim->model [sim]
  (-> sim
      (:config)
      (select-keys #{:group :response :delay :name :description})
      (source->model)))

