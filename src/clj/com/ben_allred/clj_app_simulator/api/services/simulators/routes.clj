(ns com.ben-allred.clj-app-simulator.api.services.simulators.routes
  (:require [compojure.core :as c]
            [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
            [com.ben-allred.clj-app-simulator.api.services.activity :as activity]
            [com.ben-allred.clj-app-simulator.api.utils.respond :as respond]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.utils.strings :as strings]
            [com.ben-allred.clj-app-simulator.api.services.simulators.simulators :as sims]
            [com.ben-allred.clj-app-simulator.utils.uuids :as uuids]
            [com.ben-allred.clj-app-simulator.utils.uuids :as uuids])
  (:import [java.io InputStream]))

(defn ^:privage sim->routes [f simulator]
  (->> (f simulator)
       (map (partial apply c/make-route))))

(defn receive [simulator request]
  (activity/publish :simulators/receive
                    (merge {:simulator (select-keys (common/details simulator) #{:id :config})
                            :request   (peek (common/requests simulator))}
                           request)))

(defn http-sim-route [simulator]
  (fn [request]
    (let [response (->> (update request :body #(if (instance? InputStream %)
                                                 (strings/trim-to-nil (slurp %))
                                                 %))
                        (common/receive simulator))]
      (receive simulator {})
      response)))

(defn ws-sim-route [simulator]
  (fn [request]
    (common/connect simulator request)))

(defn get-sim [simulator]
  (fn [_]
    (respond/with [:ok {:simulator (common/details simulator)}])))

(defn delete-sim [simulator delete-sim!]
  (fn [_]
    (activity/publish :simulators/delete
                      (select-keys (common/details simulator) #{:id :config}))
    (delete-sim!)
    (respond/with [:no-content])))

(defn patch-sim [simulator]
  (fn [{{:keys [action config]} :body}]
    (try (let [action (keyword action)]
           (case action
             :simulators/reset (common/reset simulator)
             :http/reset-requests (common/reset-requests simulator)
             :http/reset-response (common/reset-response simulator)
             :http/change (common/change simulator config)
             nil)
           (let [details (common/details simulator)]
             (when (#{:simulators/reset :http/reset-requests :http/reset-response :http/change} action)
               (activity/publish action details))
             (respond/with [:ok details])))
         (catch Throwable ex
           (respond/with [:bad-request (:problems (ex-data ex))])))))

(defn patch-ws [simulator]
  (let [f (patch-sim simulator)]
    #(f (assoc-in % [:body :action] :simulators/reset))))

(defn send-ws [simulator]
  (fn [{:keys [params body]}]
    (let [body (if (instance? InputStream body) (slurp body) (str body))
          socket-id (:socket-id params)]
      (if socket-id
        (common/send simulator (uuids/->uuid socket-id) body)
        (common/send simulator body))
      (respond/with [:no-content]))))

(defn disconnect-ws [simulator]
  (fn [{:keys [params]}]
    (if-let [socket-id (:socket-id params)]
      (common/disconnect simulator (uuids/->uuid socket-id))
      (common/disconnect simulator))
    (respond/with [:no-content])))

(defn http-routes [simulator]
  (let [{{:keys [method path]} :config id :id} (common/details simulator)
        method-str (name method)
        get (get-sim simulator)
        delete (delete-sim simulator #(sims/remove! method path))
        patch (patch-sim simulator)
        path (when (not= path "/") path)
        uri (str "/api/simulators/" method-str path)
        uuid-uri (str "/api/simulators/" id)]
    [[(keyword method-str) (str "/simulators" path) (http-sim-route simulator)]
     [:get uri get]
     [:get uuid-uri get]
     [:delete uri delete]
     [:delete uuid-uri delete]
     [:patch uri patch]
     [:patch uuid-uri patch]]))

(defn ws-routes [simulator]
  (let [{{:keys [path]} :config id :id} (common/details simulator)
        delete (delete-sim simulator #(sims/remove! :ws path))
        path (if (= path "/") "" path)
        sim-path (str "/simulators" path)
        uri (str "/api/simulators/ws" path)
        uuid-uri (str "/api/simulators/" id)
        socket-uri (str uri "/:socket-id")
        socket-uuid-uri (str uuid-uri "/:socket-id")
        connect (ws-sim-route simulator)
        get (get-sim simulator)
        send (send-ws simulator)
        patch (patch-ws simulator)
        disconnect (disconnect-ws simulator)]
    [[:get sim-path connect]
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
     [:delete socket-uri disconnect]
     [:delete socket-uuid-uri disconnect]
     [:patch uri patch]
     [:patch uuid-uri patch]]))

(defn http-sim->routes [simulator]
  (sim->routes http-routes simulator))

(defn ws-sim->routes [simulator]
  (sim->routes ws-routes simulator))
