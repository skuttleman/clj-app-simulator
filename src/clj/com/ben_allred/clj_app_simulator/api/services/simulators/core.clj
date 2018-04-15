(ns com.ben-allred.clj-app-simulator.api.services.simulators.core
  (:refer-clojure :exclude [set!])
  (:require [com.ben-allred.clj-app-simulator.api.services.simulators.http :as http.sim]
            [compojure.core :as c]
            [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
            [com.ben-allred.clj-app-simulator.api.services.activity :as activity]
            [com.ben-allred.clj-app-simulator.api.utils.respond :as respond]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.utils.uuids :as uuids]))

(def ^:private simulators (atom {}))

(defn ^:private remove-simulator! [method path]
  (swap! simulators dissoc [method path]))

(defn ^:private config->?simulator [config]
  (let [{:keys [method path] :as config} (update config :method keyword)]
    (when-let [simulator (when-not (contains? @simulators [method path])
                           (http.sim/->HttpSimulator (uuids/random) config))]
      (common/start simulator)
      (swap! simulators assoc [method path] simulator)
      simulator)))

(defn ^:private simulator-configs
  ([]
   (simulator-configs identity))
  ([f]
   (->> @simulators
        (vals)
        (map f)
        (assoc {} :simulators))))

(defn details []
  (->> (simulator-configs common/details)
       (conj [:ok])
       (respond/with)))

(defn add [config]
  (if-let [simulator (config->?simulator config)]
    (let [sim (common/details simulator)]
      (activity/publish :simulators/add sim)
      (respond/with [:created {:simulator sim}]))
    (respond/with [:bad-request {:message (or (http.sim/why-not? config) "simulator already exists")}])))

(defn set! [configs]
  (let [invalid-configs (remove http.sim/valid? configs)]
    (if (empty? invalid-configs)
      (do
        (reset! simulators {})
        (let [sims (->> configs
                        (map config->?simulator)
                        (map common/details)
                        (doall))]
          (activity/publish :simulators/init sims)
          (respond/with [:created (simulator-configs common/details)])))
      (respond/with [:bad-request {:message    "one more more invalid simulators"
                                   :simulators (map #(assoc % :reason (http.sim/why-not? %)) invalid-configs)}]))))

(defn reset-all! []
  (let [sims (:simulators (simulator-configs))]
    (dorun (map common/reset sims))
    (activity/publish :simulators/reset-all (map common/details sims)))
  (respond/with [:no-content]))

(defn routes []
  (->> #(common/routes % remove-simulator!)
       (simulator-configs)
       (:simulators)
       (mapcat identity)
       (apply c/routes)))
