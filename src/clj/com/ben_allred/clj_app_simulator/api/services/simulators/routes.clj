(ns com.ben-allred.clj-app-simulator.api.services.simulators.routes
  (:require [compojure.core :as c]
            [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
            [com.ben-allred.clj-app-simulator.api.services.activity :as activity]
            [com.ben-allred.clj-app-simulator.api.utils.respond :as respond]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.utils.strings :as strings])
  (:import [java.io InputStream]))

(defn sim-route [simulator]
  (fn [request]
    (let [response (->> (update request :body #(if (instance? InputStream %)
                                                 (strings/trim-to-nil (slurp %))
                                                 %))
                        (common/receive simulator))]
      (activity/publish :simulators/receive
                        {:simulator (select-keys (common/details simulator) #{:id :config})
                         :request   (peek (common/requests simulator))})
      response)))

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

(defn routes [simulator delete-sim!]
  (let [{{:keys [method path]} :config id :id} (common/details simulator)
        method-str (name method)
        get (get-sim simulator)
        delete (delete-sim simulator #(delete-sim! method path))
        patch (patch-sim simulator)
        path (when (not= path "/") path)
        uri (str "/api/simulators/" method-str path)
        uuid-uri (str "/api/simulators/" id)]
    [[(keyword method-str) (str "/simulators" path) (sim-route simulator)]
     [:get uri get]
     [:get uuid-uri get]
     [:delete uri delete]
     [:delete uuid-uri delete]
     [:patch uri patch]
     [:patch uuid-uri patch]]))

(defn http-sim->routes [simulator delete-sim!]
  (->> (routes simulator delete-sim!)
       (map (partial apply c/make-route))))
