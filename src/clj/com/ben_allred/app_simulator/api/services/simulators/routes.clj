(ns com.ben-allred.app-simulator.api.services.simulators.routes
  (:require
    [com.ben-allred.app-simulator.api.services.activity :as activity]
    [com.ben-allred.app-simulator.api.services.simulators.common :as common]
    [com.ben-allred.app-simulator.api.services.simulators.simulators :as sims]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [com.ben-allred.app-simulator.utils.strings :as strings]
    [com.ben-allred.app-simulator.utils.uuids :as uuids]
    [compojure.core :as c])
  (:import
    (java.io InputStream)))

(defn ^:private sim->routes [env f simulator]
  (->> (f env simulator)
       (map (partial apply c/make-route))))

(defn receive [env simulator]
  (activity/publish env
                    :simulators/receive
                    (merge {:simulator (common/details simulator)
                            :request   (peek (common/received simulator))})))

(defn http-sim-route [env simulator]
  (fn [request]
    (let [response (->> (update request :body #(if (instance? InputStream %)
                                                 (strings/trim-to-nil (slurp %))
                                                 %))
                        (common/receive! simulator))]
      (receive env simulator)
      response)))

(defn ws-sim-route [simulator]
  (fn [request]
    (common/connect! simulator request)))

(defn get-sim [simulator]
  (fn [_]
    [:ok {:simulator (common/details simulator)}]))

(defn delete-sim [env simulator delete-sim!]
  (fn [_]
    (activity/publish env
                      :simulators/delete
                      {:simulator (common/details simulator)})
    (delete-sim! (common/identifier simulator))
    [:no-content]))

(defn patch-sim [env simulator]
  (fn [{{:keys [action config type]} :body}]
    (try (let [action (keyword action)
               type (keyword type)]
           (case action
             :simulators/change (common/reset! simulator config)
             :simulators/reset (if type
                                 (common/partially-reset! simulator type)
                                 (common/reset! simulator))
             nil)
           (let [details (common/details simulator)]
             (when (#{:simulators/reset :simulators/change} action)
               (activity/publish env action {:simulator details}))
             [:ok {:simulator details}]))
         (catch Throwable ex
           [:bad-request (:problems (ex-data ex))]))))

(defn patch-ws [env simulator]
  (fn [{{:keys [action socket-id config type]} :body}]
    (let [action (keyword action)
          type (keyword type)
          socket-id (uuids/->uuid socket-id)]
      (case action
        :simulators/change (common/reset! simulator config)
        :simulators/reset (if type
                            (common/partially-reset! simulator type)
                            (common/reset! simulator))
        :simulators.ws/disconnect (if socket-id
                                    (common/disconnect! simulator socket-id)
                                    (common/disconnect! simulator))
        nil)
      (let [details (common/details simulator)]
        (when (#{:simulators/reset :simulators/change :simulators.ws/disconnect} action)
          (activity/publish env action (cond-> {:simulator details} socket-id (assoc :socket-id socket-id))))
        [:ok {:simulator details}]))))

(defn send-ws [simulator]
  (fn [{:keys [params body]}]
    (let [body (if (instance? InputStream body) (slurp body) (str body))
          socket-id (:socket-id params)]
      (if socket-id
        (common/send! simulator (uuids/->uuid socket-id) body)
        (common/send! simulator body))
      [:no-content])))

(defn disconnect-ws [simulator]
  (fn [_]
    (common/disconnect! simulator)
    [:no-content]))

(defn http-routes [env simulator]
  (let [{{:keys [method path]} :config id :id} (common/details simulator)
        method-str (name method)
        get (get-sim simulator)
        delete (delete-sim env simulator (partial sims/remove! env))
        patch (patch-sim env simulator)
        path (when (not= path "/") path)
        uri (str "/api/simulators/" id)]
    [[(keyword method-str) (str "/simulators" path) (http-sim-route env simulator)]
     [:get uri get]
     [:delete uri delete]
     [:patch uri patch]]))

(defn ws-routes [env simulator]
  (let [{{:keys [path]} :config id :id} (common/details simulator)
        delete (delete-sim env simulator (partial sims/remove! env))
        sim-path (str "/simulators" (when (not= path "/") path))
        uri (str "/api/simulators/" id)
        socket-uri (str uri "/sockets/:socket-id")
        get (get-sim simulator)
        send (send-ws simulator)
        patch (patch-ws env simulator)
        disconnect (disconnect-ws simulator)]
    [[:get sim-path (ws-sim-route simulator)]
     [:get uri get]
     [:delete uri delete]
     [:post uri send]
     [:post socket-uri send]
     [:delete uri disconnect]
     [:patch uri patch]]))

(defn http-sim->routes [env simulator]
  (sim->routes env http-routes simulator))

(defn ws-sim->routes [env simulator]
  (sim->routes env ws-routes simulator))

(defn file-sim->routes [env simulator]
  (sim->routes env http-routes simulator))
