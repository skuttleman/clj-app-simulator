(ns com.ben-allred.clj-app-simulator.api.services.simulators.core
    (:require [com.ben-allred.clj-app-simulator.api.services.simulators.http :as http]
              [compojure.core :as c]
              [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
              [com.ben-allred.clj-app-simulator.utils.logging :as log]
              [clojure.spec.alpha :as s]))

(def ^:private simulators (atom {}))

(defn ^:private remove-simulator [method path]
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
    (if (config->?simulator config)
        {:status 204}
        {:status 400
         :body   (s/explain-data :http/http-simulator config)}))

(defn set-simulators [configs]
    (reset! simulators {})
    (let [sims (->> configs
                   (map config->?simulator)
                   (remove nil?)
                   (doall))]
        (if (seq sims)
            {:status 204}
            {:status 400})))

(defn reset-all []
    (->> @simulators
        (map common/reset)
        (dorun))
    {:status 204})

(defn routes []
    (->> @simulators
        (vals)
        (mapcat #(common/routes % remove-simulator))
        (apply c/routes)))
