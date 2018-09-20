(ns com.ben-allred.clj-app-simulator.api.services.simulators.simulators
  (:require [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.utils.fns :as fns]))

(defonce ^:private sims (atom {}))

(defn clear! []
  (->> @sims
       (vals)
       (map common/stop)
       (dorun))
  (reset! sims {}))

(defn add! [simulator]
  (let [key (common/identifier simulator)]
    (when-not (contains? @sims key)
      (swap! sims assoc key simulator)
      (common/start simulator)
      simulator)))

(defn remove! [key]
  (when-let [simulator (get @sims key)]
    (common/stop simulator)
    (swap! sims dissoc key)))

(defn simulators []
  (->> @sims
       (sort-by (fns/=>> (first) (mapv name) (apply str)))
       (reverse)
       (map second)))
