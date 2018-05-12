(ns com.ben-allred.clj-app-simulator.api.services.simulators.store.reducers
  (:require [com.ben-allred.collaj.reducers :as collaj.reducers]
            [com.ben-allred.clj-app-simulator.utils.maps :as maps]))

(defn ^:private simulator-config
  ([] nil)
  ([state [type config]]
   (case type
     :simulators/init {:initial config :current config}
     :simulators/reset (assoc state :current (:initial state))
     state)))

(defn ^:private http-config*
  ([] nil)
  ([state [type config]]
   (case type
     :http/change (update state :current maps/deep-merge config)
     :http/reset-response (assoc state :current (:initial state))
     state)))

(defn ^:private simulator-requests
  ([] [])
  ([state [type request]]
   (case type
     :simulators/init []
     :simulators/reset []
     :simulators/receive (conj state request)
     state)))

(defn ^:private http-requests*
  ([] [])
  ([state [type]]
   (case type
     :http/reset-requests []
     state)))

(defn ^:private ws-sockets*
  ([] nil)
  ([state [type _ ws]]
   (case type
     :ws/connect ws
     :ws/remove nil
     state)))

(def http-config (collaj.reducers/comp http-config* simulator-config))

(def http-requests (collaj.reducers/comp http-requests* simulator-requests))

(def ws-sockets (collaj.reducers/map-of #(when (#{:ws/connect :ws/remove} (first %))
                                           (second %))
                                        ws-sockets*))

(def http
  (collaj.reducers/combine
    {:config   http-config
     :requests http-requests}))

(def ws
  (collaj.reducers/combine
    {:config   simulator-config
     :requests simulator-requests
     :sockets  ws-sockets}))
