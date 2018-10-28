(ns com.ben-allred.clj-app-simulator.ui.simulators.http.interactions
  (:require
    [com.ben-allred.clj-app-simulator.templates.transformations.http :as tr]
    [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as shared.interactions]
    [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(defn update-simulator [form id]
  (shared.interactions/update-simulator form tr/model->source id))

(defn reset-simulator [form id]
  (shared.interactions/reset-config form tr/sim->model id :http))

(defn create-simulator [form]
  (shared.interactions/create-simulator form tr/model->source))
