(ns com.ben-allred.clj-app-simulator.api.services.simulators.http
    (:require [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
              [com.ben-allred.clj-app-simulator.api.services.simulators.store.actions :as actions]
              [com.ben-allred.clj-app-simulator.api.services.simulators.store.core :as store]
              [com.ben-allred.clj-app-simulator.api.services.simulators.routes :as routes.sim]
              [clojure.spec.alpha :as s]
              [com.ben-allred.clj-app-simulator.api.services.activity :as activity]))

(s/def ::path (partial re-matches #"/|(/:?[A-Za-z-_0-9]+)+"))

(s/def ::method (comp #{:http/delete :http/get :http/head :http/options :http/patch :http/post :http/put} keyword))

(s/def ::delay #(>= % 0))

(s/def ::status #(<= 200 % 599))

(s/def ::headers (s/map-of string? (s/or :string string? :string-coll (s/coll-of string?))))

(s/def ::body string?)

(s/def :http.partial/response (s/keys :opt-un [::status ::headers ::body]))

(s/def :http/response (s/keys :req-un [::status]
                              :opt-un [::headers ::body]))

(s/def :http.partial/http-simulator (s/keys :opt-un [::delay :http.partial/response]))

(s/def :http/http-simulator (s/merge :http.partial/http-simulator
                                (s/keys :req-un [::path ::method :http/response])))

(defn ^:private sleep [ms]
    (Thread/sleep ms))

(defn valid? [config]
    (s/valid? :http/http-simulator config))

(defn why-not? [config]
    (s/explain-data :http/http-simulator config))

(defn why-not-update? [config]
    (s/explain-data :http.partial/http-simulator config))

(defn ->HttpSimulator [config]
    (when (valid? config)
        (let [{:keys [dispatch get-state]} (store/http-store)]
            (reify
                common/ISimulator
                (start [_]
                    (dispatch (actions/init config)))
                (stop [_])
                (receive [_ request]
                    (dispatch (actions/receive request))
                    (let [state (get-state)
                          delay (store/delay state)]
                        (when (pos-int? delay)
                            (sleep delay))
                        (store/response state)))
                (requests [_]
                    (store/requests (get-state)))
                (config [_]
                    (store/config (get-state)))
                (details [_]
                    (store/details (get-state)))
                (reset [_]
                    (dispatch actions/reset))
                (routes [this delete]
                    (routes.sim/http-sim->routes this delete))

                common/IHTTPSimulator
                (reset-requests [_]
                    (dispatch actions/reset-requests))
                (reset-response [_]
                    (dispatch actions/reset-response))
                (change [_ config]
                    (when-let [problems (why-not-update? config)]
                        (throw (ex-info "config does not conform to spec" {:problems problems})))
                    (dispatch (actions/change (select-keys config #{:delay :response}))))))))
