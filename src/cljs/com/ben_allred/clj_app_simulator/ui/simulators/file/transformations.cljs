(ns com.ben-allred.clj-app-simulator.ui.simulators.file.transformations
  (:require [com.ben-allred.clj-app-simulator.ui.simulators.shared.transformations :as shared.tr]
            [com.ben-allred.clj-app-simulator.utils.uuids :as uuids]
            [com.ben-allred.formation.core :as f]
            [com.ben-allred.clj-app-simulator.utils.fns :as fns :include-macros true]))

(def source->model
  (f/make-transformer
    [#(update % :delay (fn [delay] (or delay 0)))
     {:response {:headers (fns/=>> (mapcat (fn [[k v]]
                                             (if (coll? v)
                                               (->> v
                                                    (sort)
                                                    (map (fn [v'] [k v'])))
                                               [[k v]])))
                                   (sort-by first)
                                   (vec))}}]))

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

(-> {:response {:file :com.ben-allred.clj-app-simulator.ui.simulators.file.transformations-test/file}, :name "   name   ", :group "  group  ", :description "  description  "}
    {:response {:body :com.ben-allred.clj-app-simulator.ui.simulators.file.transformations-test/file}, :name "name", :group "group", :description "description"})
