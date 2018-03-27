(ns com.ben-allred.clj-app-simulator.api.services.simulators.core
  (:refer-clojure :exclude [set!])
  (:require [com.ben-allred.clj-app-simulator.api.services.simulators.http :as http.sim]
            [compojure.core :as c]
            [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
            [com.ben-allred.clj-app-simulator.api.services.activity :as activity]
            [com.ben-allred.clj-app-simulator.api.utils.respond :as respond]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(def ^:private simulators (atom {}))

(defn ^:private remove-simulator! [method path]
  (swap! simulators dissoc [method path]))

(defn ^:private config->?simulator [config]
  (let [{:keys [method path] :as config} (update config :method keyword)]
    (when-let [simulator (when-not (contains? @simulators [method path])
                           (http.sim/->HttpSimulator config))]
      (common/start simulator)
      (swap! simulators assoc [method path] simulator)
      simulator)))

(defn configs []
  (->> @simulators
       (vals)
       (map (comp #(dissoc % :response) common/config))
       (assoc {} :simulators)
       (conj [:ok])
       (respond/with)))

(defn add [config]
  (if-let [simulator (config->?simulator config)]
    (do
      (activity/publish :simulators/add (common/config simulator))
      (respond/with [:no-content]))

    (respond/with [:bad-request {:message (or (http.sim/why-not? config) "simulator already exists")}])))

(defn set! [configs]
  (let [invalid-configs (remove http.sim/valid? configs)]
    (if (empty? invalid-configs)
      (do
        (reset! simulators {})
        (let [sims (->> configs
                        (map config->?simulator)
                        (map common/config)
                        (doall))]
          (activity/publish :simulators/init sims)
          (respond/with [:no-content])))
      (respond/with [:bad-request {:message    "one more more invalid simulators"
                                   :simulators (map #(assoc {:config %} :reason (http.sim/why-not? %)) invalid-configs)}]))))

(defn reset-all! []
  (let [sims (->> @simulators
                  (vals))]
    (dorun (map common/reset sims))
    (activity/publish :simulators/reset-all (map common/config sims)))
  (respond/with [:no-content]))

(defn routes []
  (->> @simulators
       (vals)
       (mapcat #(common/routes % remove-simulator!))
       (apply c/routes)))
