(ns com.ben-allred.clj-app-simulator.api.services.simulators.simulators
  (:require [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(def ^:private sims (atom {}))

(defn clear! []
  (->> @sims
       (vals)
       (map common/stop)
       (dorun))
  (reset! sims {}))

(defn add! [simulator]
  (let [{{:keys [method path]} :config} (common/details simulator)]
    (when-not (contains? @sims [method path])
      (swap! sims assoc [method path] simulator)
      (common/start simulator)
      simulator)))

(defn remove! [method path]
  (when-let [simulator (get @sims [method path])]
    (common/stop simulator)
    (swap! sims dissoc [method path])))

(defn simulators []
  (vals @sims))
