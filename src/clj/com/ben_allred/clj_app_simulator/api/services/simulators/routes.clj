(ns com.ben-allred.clj-app-simulator.api.services.simulators.routes
  (:require [compojure.core :as c]
            [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
            [com.ben-allred.clj-app-simulator.api.services.activity :as activity]
            [com.ben-allred.clj-app-simulator.api.utils.respond :as respond]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(defn ^:private update-sim [simulator {:keys [action config]}]
  (let [action (keyword action)]
    (when (#{:simulator/reset :http/reset-requests :http/reset-response :http/change}
            action)
      (case action
        :simulator/reset (common/reset simulator)
        :http/reset-requests (common/reset-requests simulator)
        :http/reset-response (common/reset-response simulator)
        :http/change (common/change simulator config)
        nil)
      (let [details (common/details simulator)]
        (activity/publish action details)
        details))))

(defn http-sim->routes [simulator delete]
  (let [{{:keys [method path]} :config id :id} (common/details simulator)
        method-str (name method)
        uri (str "/api/simulators/" method-str path)
        uuid-uri (str "/api/simulators/" id)
        get (fn [_]
              (respond/with [:ok {:simulator (common/details simulator)}]))
        delete (fn [_]
                 (activity/publish :simulators/delete
                                   (select-keys (common/details simulator) #{:id :config}))
                 (delete method path)
                 (respond/with [:no-content]))
        patch (fn [{:keys [body]}]
                (try (respond/with [:ok (update-sim simulator body)])
                     (catch Throwable ex
                       (respond/with [:bad-request (:problems (ex-data ex))]))))]
    (->> [[(keyword method-str)
           (str "/simulators" path)
           (fn [request]
             (let [response (common/receive simulator request)]
               (activity/publish :simulators/receive
                                 {:simulator (select-keys (common/details simulator) #{:id :config})
                                  :request   (peek (common/requests simulator))})
               response))]
          [:get uri get]
          [:get uuid-uri get]
          [:delete uri delete]
          [:delete uuid-uri delete]
          [:patch uri patch]
          [:patch uuid-uri patch]]
         (map (partial apply c/make-route)))))
