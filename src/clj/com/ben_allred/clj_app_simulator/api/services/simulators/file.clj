(ns com.ben-allred.clj-app-simulator.api.services.simulators.file
  (:require [clojure.spec.alpha :as s]
            [com.ben-allred.clj-app-simulator.api.services.resources.core :as resources]
            [com.ben-allred.clj-app-simulator.api.services.simulators.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
            [com.ben-allred.clj-app-simulator.api.services.simulators.store.core :as store]
            [com.ben-allred.clj-app-simulator.api.services.simulators.routes :as routes.sim]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [clojure.string :as string]))

(s/def ::path (partial re-matches #"/|(/:?[A-Za-z-_0-9]+)+"))

(s/def ::method (s/conformer (comp #(or % ::s/invalid)
                                   #{:file/delete :file/get :file/head :file/options :file/patch :file/post :file/put}
                                   keyword)))

(s/def ::delay #(>= % 0))

(s/def ::status #(<= 200 % 599))

(s/def ::headers (s/map-of (s/conformer keyword)
                           (fn [v]
                             (or (string? v)
                                 (and (coll? v)
                                      (every? string? v))))
                           :conform-keys true))

(s/def ::file #'resources/has-upload?)

(s/def :file.partial/response (s/keys :opt-un [::status ::headers ::file]))

(s/def :file/response (s/keys :req-un [::status ::file]
                              :opt-un [::headers]))

(s/def :file.partial/file-simulator (s/keys :opt-un [::delay :file.partial/response]))

(s/def :file/file-simulator (s/merge :file.partial/file-simulator
                                     (s/keys :req-un [::path ::method :file/response])))

(defn ^:private sleep [ms]
  (Thread/sleep ms))

(defn ^:private conform-to [spec config]
  (let [conformed (s/conform spec config)]
    (when-not (= :clojure.spec.alpha/invalid conformed)
      conformed)))

(defn valid? [config]
  (s/valid? :file/file-simulator config))

(defn why-not-update? [config]
  (s/explain-data :file.partial/file-simulator config))

(defn ->FileSimulator [env id config]
  (when-let [{:keys [method path] :as config} (conform-to :file/file-simulator config)]
    (let [{:keys [dispatch get-state]} (store/file-store)
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
            (store/file-response env state)))
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
          (if-let [config (conform-to :file.partial/file-simulator config)]
            (dispatch (actions/change (dissoc config :method :path)))
            (throw (ex-info "config does not conform to spec" {:problems (why-not-update? config)}))))

        common/IRoute
        (routes [this]
          (routes.sim/file-sim->routes env this))

        common/IPartiallyReset
        (partially-reset! [_ type]
          (case type
            :requests (dispatch actions/reset-requests)
            :response (dispatch actions/reset-response)))))))
