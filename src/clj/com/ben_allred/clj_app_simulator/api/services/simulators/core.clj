(ns com.ben-allred.clj-app-simulator.api.services.simulators.core
    (:require [com.ben-allred.clj-app-simulator.api.services.simulators.http :as http]
              [compojure.core :as c]
              [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
              [com.ben-allred.clj-app-simulator.utils.logging :as log]
              [clojure.spec.alpha :as s]
              [com.ben-allred.clj-app-simulator.api.services.activity :as activity]))

(def ^:private simulators (atom {}))

(defn ^:private remove-simulator! [method path]
    (swap! simulators dissoc [method path]))

(defn ^:private config->?simulator [config]
    (let [{:keys [method path] :as config} (update config :method keyword)]
        (when-let [simulator (when-not (contains? @simulators [method path])
                                 (http/->HttpSimulator config))]
            (common/start simulator)
            (swap! simulators assoc [method path] simulator)
            simulator)))

(defn route-configs []
    (->> @simulators
        (vals)
        (map common/config)
        (assoc-in {:status 200} [:body :simulators])))

(defn add-simulator [config]
    (if-let [simulator (config->?simulator config)]
        (do
            (activity/publish :simulators/add (common/config simulator))
            {:status 204})
        {:status 400
         :body   (s/explain-data :http/http-simulator config)}))

(defn set-simulators [configs]
    (reset! simulators {})
    (let [sims (->> configs
                   (map config->?simulator)
                   (remove nil?)
                   (doall))]
        (activity/publish :simulators/init (map common/config sims))
        {:status 204}))

(defn reset-all []
    (->> @simulators
        (map common/reset)
        (dorun))
    (activity/publish :simulators/reset-all nil)
    {:status 204})

(defn routes []
    (->> @simulators
        (vals)
        (mapcat #(common/routes % remove-simulator!))
        (apply c/routes)))
