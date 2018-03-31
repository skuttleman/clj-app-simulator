(ns com.ben-allred.clj-app-simulator.api.services.simulators.routes
  (:require [compojure.core :as c]
            [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
            [com.ben-allred.clj-app-simulator.api.services.activity :as activity]
            [com.ben-allred.clj-app-simulator.api.utils.respond :as respond]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(defn ^:private update-sim [simulator {:keys [action config]}]
  (let [action (keyword action)]
    (case action
      :simulator/reset (do (common/reset simulator)
                           (activity/publish action (common/config simulator)))
      :http/reset-requests (do (common/reset-requests simulator)
                               (activity/publish action (-> simulator
                                                            (common/config)
                                                            (select-keys #{:path :method}))))
      :http/reset-response (do (common/reset-response simulator)
                               (activity/publish action (common/config simulator)))
      :http/change (do (common/change simulator config)
                       (activity/publish action (common/config simulator)))
      nil)))

(defn http-sim->routes [simulator delete]
  (let [{:keys [method path]} (common/config simulator)
        method-str (name method)
        uri (str "/api/simulators/" method-str path)]
    (->> [[(keyword method-str)
           (str "/simulators" path)
           (fn [request]
             (let [response (common/receive simulator request)]
               (activity/publish :simulators/receive
                                 {:simulator (select-keys (common/config simulator) #{:method :path})
                                  :request   (peek (common/requests simulator))})
               response))]
          [:get uri (fn [_]
                      (respond/with [:ok {:simulator (common/details simulator)}]))]
          [:delete uri (fn [_]
                         (activity/publish :simulators/delete
                                           (select-keys (common/config simulator) #{:method :path}))
                         (delete method path)
                         (respond/with [:no-content]))]
          [:patch uri (fn [{:keys [body]}]
                        (try (update-sim simulator body)
                             (respond/with [:no-content])
                             (catch Throwable ex
                               (respond/with [:bad-request (:problems (ex-data ex))]))))]]
         (map (partial apply c/make-route)))))
