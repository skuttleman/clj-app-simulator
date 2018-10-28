(ns com.ben-allred.app-simulator.api.services.simulators.http
  (:require
    [clojure.string :as string]
    [com.ben-allred.app-simulator.api.services.simulators.common :as common]
    [com.ben-allred.app-simulator.api.services.simulators.routes :as routes.sim]
    [com.ben-allred.app-simulator.api.services.simulators.store.actions :as actions]
    [com.ben-allred.app-simulator.api.services.simulators.store.core :as store]
    [com.ben-allred.app-simulator.api.utils.specs :as specs]
    [com.ben-allred.app-simulator.utils.logging :as log]))

(defn ^:private sleep [ms]
  (Thread/sleep ms))

(defn valid? [config]
  (specs/valid? :simulator.http/config config))

(defn ->HttpSimulator [env id config]
  (when-let [{:keys [method path] :as config} (specs/conform :simulator.http/config config)]
    (let [{:keys [dispatch get-state]} (store/http-store)
          id-path (string/replace path #":[^/]+" "*")]
      (dispatch (actions/init config))
      (reify
        common/IReceive
        (receive! [_ request]
          (dispatch (actions/receive request))
          (let [state (get-state)
                delay (store/delay state)]
            (when (pos-int? delay)
              (sleep delay))
            (store/response state)))
        (received [_]
          (store/requests (get-state)))

        common/IIdentify
        (details [_]
          (-> (get-state)
              (store/details)
              (assoc :id id)))
        (identifier [_]
          [(keyword (name method)) id-path])

        common/IReset
        (reset! [_]
          (dispatch actions/reset))
        (reset! [_ config]
          (if-let [config (specs/conform :simulator.http.change/config config)]
            (dispatch (actions/change (dissoc config :method :path)))
            (throw (ex-info "config does not conform to spec"
                            {:problems (specs/explain :simulator.http.change/config config)}))))

        common/IRoute
        (routes [this]
          (routes.sim/http-sim->routes env this))

        common/IPartiallyReset
        (partially-reset! [_ type]
          (case type
            :http/requests (dispatch actions/reset-requests)
            :http/response (dispatch actions/reset-response)
            :http/config (dispatch actions/reset-config)))))))
