(ns com.ben-allred.clj-app-simulator.api.services.simulators.http
    (:require [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
              [com.ben-allred.clj-app-simulator.api.services.simulators.store.actions :as actions]
              [com.ben-allred.clj-app-simulator.api.services.simulators.store.core :as store]
              [clojure.spec.alpha :as s]
              [com.ben-allred.clj-app-simulator.utils.maps :as maps]
              [compojure.core :as c]
              [com.ben-allred.clj-app-simulator.utils.logging :as log]
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

(defn ^:private update-sim [simulator body]
    (case (:action body)
        :simulator/reset (common/reset simulator)
        :http/reset-requests (common/reset-requests simulator)
        :http/reset-response (common/reset-response simulator)
        :http/change (common/change simulator (:config body))
        nil))

(defn ^:private config->routes [simulator delete]
    (let [{:keys [method path] :as config} (common/config simulator)
          method-str (name method)
          uri        (str "/api/simulators/" method-str path)
          method'     (keyword method-str)]
        (->> [[method' (str "/simulators" path) (fn [request]
                                                   (let [response (common/receive simulator request)]
                                                       (activity/publish :simulators/recieve
                                                                         {:config  (common/config simulator)
                                                                          :request (pop (common/requests simulator))})
                                                       response))]
              [:get uri (fn [_]
                            {:status 200
                             :body   (common/details simulator)})]
              [:delete uri (fn [_]
                               (activity/publish :simulators/delete
                                                 {:config (common/config simulator)})
                               (delete method path)
                               {:status 204})]
              [:patch uri (fn [{:keys [body]}]
                              (try (update-sim simulator body)
                                   {:status 204}
                                   (catch Throwable ex
                                       {:status 400 :body (:problems (ex-data ex))})))]]
            (map (partial apply c/make-route)))))

(defn ->HttpSimulator [config]
    (let [{:keys [dispatch get-state]} (store/http-store)]
        (when (s/valid? :http/http-simulator config)
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
                            (Thread/sleep delay))
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
                    (config->routes this delete))

                common/IHTTPSimulator
                (reset-requests [_]
                    (dispatch actions/reset-requests))
                (reset-response [_]
                    (dispatch actions/reset-response))
                (change [_ config]
                    (when-let [problems (s/explain-data :http.partial/http-simulator config)]
                        (throw (ex-info "config does not conform to spec" {:problems problems})))
                    (dispatch (actions/change (select-keys config #{:response :delay}))))))))
