(ns com.ben-allred.clj-app-simulator.ui.simulators.ws.interactions
  (:require [com.ben-allred.clj-app-simulator.ui.simulators.ws.transformations :as tr]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as shared.interactions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]))


(defn update-simulator [form id submittable?]
  (shared.interactions/update-simulator form tr/model->source id submittable?))

(defn reset-simulator [form id]
  (shared.interactions/reset-simulator form tr/sim->model id))

(defn create-simulator [form submittable?]
  (shared.interactions/create-simulator form tr/model->source submittable?))

(defn disconnect-all [id]
  (fn [_]
    (shared.interactions/do-request (store/dispatch (actions/disconnect-all id)))))
