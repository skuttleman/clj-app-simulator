(ns com.ben-allred.clj-app-simulator.templates.transformations.http
  (:require [com.ben-allred.clj-app-simulator.templates.transformations.shared :as shared.tr]
            [com.ben-allred.clj-app-simulator.utils.fns :as fns :include-macros true]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.utils.strings :as strings]
            [com.ben-allred.formation.core :as f]))

(def source->model
  (f/make-transformer
    [(fns/=> (update :delay fns/or 0)
             (update-in [:response :body] identity))
     {:response {:headers (fns/=>> (mapcat (fn [[k v]]
                                             (if (coll? v)
                                               (->> v
                                                    (sort)
                                                    (map (fn [v'] [k v'])))
                                               [[k v]])))
                                   (sort-by first)
                                   (vec))}}]))

(def model->view shared.tr/model->view)

(def view->model shared.tr/view->model)

(def model->source
  (f/make-transformer
    [{:response [{:body strings/trim-to-nil}
                 shared.tr/headers->source]}
     shared.tr/model->source]))

(defn sim->model [sim]
  (-> sim
      (:config)
      (select-keys #{:group :response :delay :name :description})
      (source->model)))