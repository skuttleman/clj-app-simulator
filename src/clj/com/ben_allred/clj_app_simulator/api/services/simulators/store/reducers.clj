(ns com.ben-allred.clj-app-simulator.api.services.simulators.store.reducers
  (:require [com.ben-allred.collaj.reducers :as collaj.reducers]
            [com.ben-allred.clj-app-simulator.utils.maps :as maps]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(defn ^:private http-config*
  ([] nil)
  ([state [type]]
   (case type
     :http/reset-response (assoc state :current (:initial state))
     state)))

(defn ^:private ws-sockets*
  ([] nil)
  ([state [type _ ws]]
   (case type
     :ws/connect ws
     :ws/remove nil
     state)))

(defn simulator-config
  ([] nil)
  ([state [type config]]
   (case type
     :simulators/init {:initial config :current config}
     :simulators/reset (assoc state :current (:initial state))
     :simulators/change (update state :current maps/deep-merge config)
     state)))

(defn simulator-requests
  ([] [])
  ([state [type request]]
   (case type
     :simulators/init []
     :simulators/reset []
     :simulators/reset-requests []
     :simulators/receive (conj state request)
     state)))

(def http-config
  (collaj.reducers/comp http-config* simulator-config))

(def ws-sockets
  (collaj.reducers/comp
    (collaj.reducers/map-of #(when (#{:ws/connect :ws/remove} (first %))
                               (second %))
                            ws-sockets*)
    (fn
      ([] {})
      ([state [type]]
       (case type
         :simulators/reset {}
         state)))))

(def http
  (collaj.reducers/combine
    {:config   http-config
     :requests simulator-requests}))

(def ws
  (collaj.reducers/combine
    {:config   simulator-config
     :requests simulator-requests
     :sockets  ws-sockets}))
