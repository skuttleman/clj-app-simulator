(ns com.ben-allred.clj-app-simulator.api.services.simulators.ws
  (:require [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
            [com.ben-allred.clj-app-simulator.api.services.simulators.store.core :as store]
            [clojure.spec.alpha :as s]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.api.services.simulators.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.api.services.simulators.routes :as routes.sim]
            [immutant.web.async :as web.async]
            [com.ben-allred.clj-app-simulator.utils.uuids :as uuids]
            [com.ben-allred.clj-app-simulator.api.services.activity :as activity])
  (:import [java.io InputStream]))

;(s/def ::path (partial re-matches #"/|(/[A-Za-z-_0-9]+)+"))
(s/def ::path (partial re-matches #"/|(/:?[A-Za-z-_0-9]+)+"))

(s/def ::method (s/conformer (comp #(or % ::s/invalid) #{:ws} keyword)))

(s/def :ws/ws-simulator (s/keys :req-un [::path ::method]))

(defn ^:private conform-to [spec config]
  (let [conformed (s/conform spec config)]
    (when-not (= :clojure.spec.alpha/invalid conformed)
      conformed)))

(defn valid? [config]
  (try
    (s/valid? :ws/ws-simulator config)
    (catch Throwable _
      false)))

(defn on-open [simulator _ store ws]
  (let [{:keys [dispatch]} store
        socket-id (uuids/random)]
    (dispatch (actions/connect socket-id ws))
    (activity/publish :ws/connect (assoc (common/details simulator)
                                         :socket-id socket-id))))

(defn on-message [simulator request store ws message]
  (let [{:keys [get-state]} store
        {:keys [query-params route-params headers]} request]
    (when-let [socket-id (actions/find-socket-id (get-state) ws)]
      (common/receive simulator {:headers      headers
                                 :query-params query-params
                                 :route-params route-params
                                 :socket-id    socket-id
                                 :body         message}))))

(defn on-close [simulator _ store ws _]
  (let [{:keys [dispatch get-state]} store]
    (when-let [socket-id (actions/find-socket-id (get-state) ws)]
      (dispatch (actions/remove-socket socket-id))
      (activity/publish :ws/disconnect (assoc (common/details simulator)
                                              :socket-id socket-id)))))

(defn ->WsSimulator [id config]
  (when-let [config (conform-to :ws/ws-simulator config)]
    (let [{:keys [dispatch get-state] :as store} (store/ws-store)]
      (dispatch (actions/init config))
      (reify
        common/ISimulator
        (start [_])
        (stop [_]
          (dispatch actions/disconnect-all))
        (receive [this request]
          (dispatch (actions/receive request))
          (routes.sim/receive this (select-keys request #{:socket-id})))
        (requests [_]
          (store/messages (get-state)))
        (details [_]
          (-> (get-state)
              (store/details)
              (assoc :id id)))
        (reset [_]
          (dispatch actions/disconnect-all)
          (dispatch actions/reset))
        (routes [this]
          (routes.sim/ws-sim->routes this))

        common/IWSSimulator
        (connect [this {:keys [websocket?] :as request}]
          (when websocket?
            (web.async/as-channel
              request
              {:on-open    (partial on-open this request store)
               :on-message (partial on-message this request store)
               :on-close   (partial on-close this request store)})))
        (reset-messages [_]
          (dispatch actions/reset-messages))
        (disconnect [_]
          (dispatch actions/disconnect-all))
        (disconnect [_ socket-id]
          (dispatch (actions/disconnect socket-id)))
        (send [_ message]
          (dispatch (actions/send-all message)))
        (send [_ socket-id message]
          (dispatch (actions/send-one socket-id message)))))))
