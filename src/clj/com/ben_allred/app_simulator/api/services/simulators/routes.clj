(ns com.ben-allred.app-simulator.api.services.simulators.routes
  (:require
    [com.ben-allred.app-simulator.api.services.activity :as activity]
    [com.ben-allred.app-simulator.api.services.simulators.common :as common]
    [com.ben-allred.app-simulator.api.services.simulators.simulators :as sims]
    [com.ben-allred.app-simulator.api.services.streams :as streams]
    [com.ben-allred.app-simulator.api.utils.respond :as respond]
    [com.ben-allred.app-simulator.api.utils.specs :as specs]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [com.ben-allred.app-simulator.utils.strings :as strings]
    [com.ben-allred.app-simulator.utils.uuids :as uuids]
    [compojure.core :as c]))

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
    (let [response (->> (update request :body #(if (streams/input-stream? %)
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
    [:http.status/ok {:simulator (common/details simulator)}]))

(defn delete-sim [env simulator delete-sim!]
  (fn [_]
    (activity/publish env
                      :simulators/delete
                      {:simulator (common/details simulator)})
    (delete-sim! (common/identifier simulator))
    [:http.status/no-content]))

(defn patch [env simulator]
  (fn [{body :body}]
    (let [sim-type (common/type simulator)
          spec (keyword (format "simulator.%s/patch" (name sim-type)))]
      (if-let [{:keys [action socket-id config type]} (specs/conform spec body)]
        (do
          (case action
            :simulators/change (common/reset! simulator config)
            :simulators/reset (if type
                                (common/partially-reset! simulator type)
                                (common/reset! simulator))
            :simulators.ws/disconnect (if socket-id
                                        (common/disconnect! simulator socket-id)
                                        (common/disconnect! simulator)))
          (let [details {:simulator (common/details simulator)}]
            (activity/publish env action (cond-> details
                                           (and (= :simulators.ws/disconnect action) socket-id)
                                           (assoc :socket-id socket-id)))
            [:http.status/ok details]))
        (respond/abort! :simulators.change/failed-spec)))))

(defn send-ws [simulator]
  (fn [{:keys [params body]}]
    (let [body (if (streams/input-stream? body) (slurp body) (str body))
          socket-id (:socket-id params)]
      (if socket-id
        (common/send! simulator (uuids/->uuid socket-id) body)
        (common/send! simulator body))
      [:http.status/no-content])))

(defn disconnect-ws [simulator]
  (fn [_]
    (common/disconnect! simulator)
    [:http.status/no-content]))

(defn http-routes [env simulator]
  (let [{{:keys [method path]} :config id :id} (common/details simulator)
        method-str (name method)
        get (get-sim simulator)
        delete (delete-sim env simulator (partial sims/remove! env))
        patch (patch env simulator)
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
        patch (patch env simulator)
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
