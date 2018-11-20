(ns com.ben-allred.app-simulator.templates.transformations.http
  (:require
    [com.ben-allred.app-simulator.templates.transformations.shared :as shared.tr]
    [com.ben-allred.app-simulator.utils.fns :as fns]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [com.ben-allred.app-simulator.utils.strings :as strings]
    [com.ben-allred.formation.core :as f]))

(def source->model
  (f/make-transformer
    (conj shared.tr/source->model*
          (fns/=>
            (update-in [:response :body] identity)
            (select-keys #{:group :response :delay :name :description})))))

(def model->view shared.tr/model->view)

(def view->model shared.tr/view->model)

(def model->source
  (f/make-transformer
    [{:response [{:body strings/trim-to-nil}
                 shared.tr/headers->source]}
     shared.tr/model->source]))
