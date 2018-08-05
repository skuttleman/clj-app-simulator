(ns com.ben-allred.clj-app-simulator.ui.simulators.http.interactions
  (:require [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as shared.interactions]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.transformations :as tr]))

(defn update-simulator [form id submittable?]
  (shared.interactions/update-simulator form tr/model->source id submittable?))

(defn reset-simulator [form id]
  (shared.interactions/reset-simulator form tr/sim->model id))

(defn create-simulator [form submittable?]
  (shared.interactions/create-simulator form tr/model->source submittable?))
