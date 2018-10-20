(ns com.ben-allred.clj-app-simulator.api.services.simulators.routes
  (:require
    [com.ben-allred.clj-app-simulator.api.services.activity :as activity]
    [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
    [com.ben-allred.clj-app-simulator.api.services.simulators.simulators :as sims]
    [com.ben-allred.clj-app-simulator.utils.logging :as log]
    [com.ben-allred.clj-app-simulator.utils.strings :as strings]
    [com.ben-allred.clj-app-simulator.utils.uuids :as uuids]
    [compojure.core :as c])
  (:import
    (java.io InputStream)))

(defn ^:privage sim->routes [env f simulator]
  (->> (f env simulator)
       (map (partial apply c/make-route))))

(defn receive [env simulator request]
  (activity/publish env
                    :simulators/receive
                    (merge {:simulator (select-keys (common/details simulator) #{:id :config})
                            :request   (peek (common/received simulator))}
                           request)))

(defn http-sim-route [env simulator]
  (fn [request]
    (let [response (->> (update request :body #(if (instance? InputStream %)
                                                 (strings/trim-to-nil (slurp %))
                                                 %))
                        (common/receive! simulator))]
      (receive env simulator {})
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
                      (select-keys (common/details simulator) #{:id :config}))
    (delete-sim! (common/identifier simulator))
    [:no-content]))

(defn patch-sim [env simulator]
  (fn [{{:keys [action config]} :body}]
    (try (let [action (keyword action)]
           (case action
             :simulators/reset (common/reset! simulator)
             :simulators/change (common/reset! simulator config)
             :simulators.http/reset-requests (common/partially-reset! simulator :requests)
             :simulators.http/reset-response (common/partially-reset! simulator :response)
             nil)
           (let [details (common/details simulator)]
             (when (#{:simulators/reset :simulators/change :simulators.http/reset-requests :simulators.http/reset-response} action)
               (activity/publish env action details))
             [:ok details]))
         (catch Throwable ex
           [:bad-request (:problems (ex-data ex))]))))

(defn patch-ws [env simulator]
  (fn [{{:keys [action socket-id config]} :body}]
    (let [action (keyword action)
          socket-id (uuids/->uuid socket-id)]
      (case action
        :simulators/reset (common/reset! simulator)
        :simulators/change (common/reset! simulator config)
        :simulators.ws/reset-messages (common/partially-reset! simulator :messages)
        :simulators.ws/disconnect-all (common/disconnect! simulator)
        :simulators.ws/disconnect (common/disconnect! simulator socket-id)
        nil)
      (let [details (cond-> (common/details simulator)
                      socket-id (assoc :socket-id socket-id))]
        (when (#{:simulators/reset :simulators.ws/reset-messages :simulators/change} action)
          (activity/publish env action details))
        [:ok details]))))

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
        uri (str "/api/simulators/" method-str path)
        uuid-uri (str "/api/simulators/" id)]
    [[(keyword method-str) (str "/simulators" path) (http-sim-route env simulator)]
     [:get uri get]
     [:get uuid-uri get]
     [:delete uri delete]
     [:delete uuid-uri delete]
     [:patch uri patch]
     [:patch uuid-uri patch]]))

(defn ws-routes [env simulator]
  (let [{{:keys [path]} :config id :id} (common/details simulator)
        delete (delete-sim env simulator (partial sims/remove! env))
        path (if (= path "/") "" path)
        sim-path (str "/simulators" path)
        uri (str "/api/simulators/ws" path)
        uuid-uri (str "/api/simulators/" id)
        socket-uri (str uri "/sockets/:socket-id")
        socket-uuid-uri (str uuid-uri "/sockets/:socket-id")
        get (get-sim simulator)
        send (send-ws simulator)
        patch (patch-ws env simulator)
        disconnect (disconnect-ws simulator)]
    [[:get sim-path (ws-sim-route simulator)]
     [:get uri get]
     [:get uuid-uri get]
     [:delete uri delete]
     [:delete uuid-uri delete]
     [:post uri send]
     [:post uuid-uri send]
     [:post socket-uri send]
     [:post socket-uuid-uri send]
     [:delete uri disconnect]
     [:delete uuid-uri disconnect]
     [:patch uri patch]
     [:patch uuid-uri patch]]))

(defn http-sim->routes [env simulator]
  (sim->routes env http-routes simulator))

(defn ws-sim->routes [env simulator]
  (sim->routes env ws-routes simulator))

(defn file-sim->routes [env simulator]
  (sim->routes env http-routes simulator))
