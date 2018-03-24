(ns com.ben-allred.clj-app-simulator.api.services.simulators.routes
    (:require [compojure.core :as c]
              [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
              [com.ben-allred.clj-app-simulator.api.services.activity :as activity]
              [com.ben-allred.clj-app-simulator.api.utils.respond :as respond]))

(defn ^:private update-sim [simulator {:keys [action config]}]
    (case (keyword action)
        :simulator/reset (common/reset simulator)
        :http/reset-requests (common/reset-requests simulator)
        :http/reset-response (common/reset-response simulator)
        :http/change (common/change simulator config)
        nil))

(defn http-sim->routes [simulator delete]
    (let [{:keys [method path]} (common/config simulator)
          method-str (name method)
          uri        (str "/api/simulators/" method-str path)]
        (->> [[(keyword method-str)
               (str "/simulators" path)
               (fn [request]
                   (let [response (common/receive simulator request)]
                       (activity/publish :simulators/receive
                                         {:config  (common/config simulator)
                                          :request (peek (common/requests simulator))})
                       response))]
              [:get uri (fn [_]
                            (respond/with [:ok (common/details simulator)]))]
              [:delete uri (fn [_]
                               (activity/publish :simulators/delete
                                                 {:config (common/config simulator)})
                               (delete method path)
                               (respond/with [:no-content]))]
              [:patch uri (fn [{:keys [body]}]
                              (try (update-sim simulator body)
                                   (respond/with [:no-content])
                                   (catch Throwable ex
                                       (respond/with [:bad-request (:problems (ex-data ex))]))))]]
             (map (partial apply c/make-route)))))
