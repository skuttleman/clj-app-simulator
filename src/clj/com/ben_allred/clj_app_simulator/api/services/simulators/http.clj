(ns com.ben-allred.clj-app-simulator.api.services.simulators.http
  (:require [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
            [com.ben-allred.clj-app-simulator.api.services.simulators.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.api.services.simulators.store.core :as store]
            [com.ben-allred.clj-app-simulator.api.services.simulators.routes :as routes.sim]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]))

(s/def ::path (partial re-matches #"/|(/:?[A-Za-z-_0-9]+)+"))

(s/def ::method (s/conformer (comp #(or % ::s/invalid)
                                   #{:http/delete :http/get :http/head :http/options :http/patch :http/post :http/put}
                                   keyword)))

(s/def ::delay #(>= % 0))

(s/def ::status #(<= 200 % 599))

(s/def ::headers (s/map-of (s/conformer keyword)
                           (fn [v]
                             (or (string? v)
                                 (and (coll? v)
                                      (every? string? v))))
                           :conform-keys true))

(s/def ::body string?)

(s/def :http.partial/response (s/keys :opt-un [::status ::headers ::body]))

(s/def :http/response (s/keys :req-un [::status]
                              :opt-un [::headers ::body]))

(s/def :http.partial/http-simulator (s/keys :opt-un [::delay :http.partial/response]))

(s/def :http/http-simulator (s/merge :http.partial/http-simulator
                                     (s/keys :req-un [::path ::method :http/response])))

(defn ^:private sleep [ms]
  (Thread/sleep ms))

(defn ^:private conform-to [spec config]
  (let [conformed (s/conform spec config)]
    (when-not (= :clojure.spec.alpha/invalid conformed)
      conformed)))

(defn valid? [config]
  (s/valid? :http/http-simulator config))

(defn why-not-update? [config]
  (s/explain-data :http.partial/http-simulator config))

(defn ->HttpSimulator [env id config]
  (when-let [{:keys [method path] :as config} (conform-to :http/http-simulator config)]
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
          (if-let [config (conform-to :http.partial/http-simulator config)]
            (dispatch (actions/change (dissoc config :method :path)))
            (throw (ex-info "config does not conform to spec" {:problems (why-not-update? config)}))))

        common/IRoute
        (routes [this]
          (routes.sim/http-sim->routes env this))

        common/IPartiallyReset
        (partially-reset! [_ type]
          (case type
            :requests (dispatch actions/reset-requests)
            :response (dispatch actions/reset-response)))))))
