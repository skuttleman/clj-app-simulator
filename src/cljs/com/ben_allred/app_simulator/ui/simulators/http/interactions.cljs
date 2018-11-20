(ns com.ben-allred.app-simulator.ui.simulators.http.interactions
  (:require
    [com.ben-allred.app-simulator.templates.transformations.http :as tr]
    [com.ben-allred.app-simulator.ui.simulators.shared.interactions :as shared.interactions]
    [com.ben-allred.app-simulator.utils.logging :as log]))

(defn update-simulator [form id]
  (shared.interactions/update-simulator form tr/model->source tr/source->model id))

(defn reset-simulator [id]
  (shared.interactions/reset-config tr/source->model id :http))

(defn create-simulator [form]
  (shared.interactions/create-simulator form tr/model->source))
