(ns com.ben-allred.clj-app-simulator.api.services.simulators.simulators
  (:require [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.utils.fns :as fns]))

(defonce ^:private sims (atom {}))

(defn clear! [env]
  (->> @sims
       (env)
       (vals)
       (map common/stop)
       (dorun))
  (swap! sims dissoc env))

(defn add! [env simulator]
  (let [key (common/identifier simulator)]
    (when-not (contains? (env @sims) key)
      (swap! sims assoc-in [env key] simulator)
      (common/start simulator)
      simulator)))

(defn remove! [env key]
  (when-let [simulator (get-in @sims [env key])]
    (common/stop simulator)
    (swap! sims update env dissoc key)))

(defn simulators [env]
  (->> @sims
       (env)
       (sort-by (fns/=>> (first) (mapv name) (apply str)))
       (reverse)
       (map second)))
