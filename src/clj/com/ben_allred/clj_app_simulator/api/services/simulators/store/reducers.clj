(ns com.ben-allred.clj-app-simulator.api.services.simulators.store.reducers
  (:require
    [com.ben-allred.clj-app-simulator.utils.logging :as log]
    [com.ben-allred.collaj.reducers :as collaj.reducers]))

(defn ^:private http-config*
  ([] nil)
  ([state [type]]
   (case type
     :simulators.http/reset-response (assoc state :current (:initial state))
     state)))

(defn ^:private ws-sockets*
  ([] nil)
  ([state [type _ ws]]
   (case type
     :simulators.ws/connect ws
     :simulators.ws/remove nil
     state)))

(defn simulator-config
  ([] nil)
  ([state [type config]]
   (case type
     :simulators/init {:initial config :current config}
     :simulators/reset (assoc state :current (:initial state))
     :simulators/change (-> state
                            (update :current merge (dissoc config :response))
                            (update-in [:current :response] merge (:response config)))
     state)))

(defn ^:private simulator-requests
  ([] [])
  ([state [type request]]
   (case type
     :simulators/init []
     :simulators/reset []
     :simulators/receive (conj state request)
     state)))

(def http-requests
  (collaj.reducers/comp simulator-requests
                        (fn
                          ([] [])
                          ([state [type]]
                           (case type
                             :simulators.http/reset-requests []
                             state)))))

(def ws-requests
  (collaj.reducers/comp simulator-requests
                        (fn
                          ([] [])
                          ([state [type]]
                           (case type
                             :simulators.ws/reset-messages []
                             state)))))

(def http-config
  (collaj.reducers/comp http-config* simulator-config))

(def ws-sockets
  (collaj.reducers/comp
    (collaj.reducers/map-of #(when (#{:simulators.ws/connect :simulators.ws/remove} (first %))
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
     :requests http-requests}))

(def ws
  (collaj.reducers/combine
    {:config   simulator-config
     :requests ws-requests
     :sockets  ws-sockets}))
