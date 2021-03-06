(ns com.ben-allred.app-simulator.api.services.simulators.store.core
  (:refer-clojure :exclude [delay])
  (:require
    [com.ben-allred.app-simulator.api.services.resources.core :as resources]
    [com.ben-allred.app-simulator.api.services.simulators.store.reducers :as reducers]
    [com.ben-allred.app-simulator.api.services.streams :as streams]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [com.ben-allred.app-simulator.utils.maps :as maps]
    [com.ben-allred.collaj.core :as collaj]
    [com.ben-allred.collaj.enhancers :as collaj.enhancers]))

(defn ^:private with-file [response env]
  (if-let [resource (resources/get env (:file response))]
    (-> response
        (dissoc :file)
        (update :headers assoc
                "Content-Type" (:content-type resource)
                "Content-Length" (:size resource)
                "Content-Disposition" (format "inline; filename=\"%s\"" (:filename resource)))
        (assoc :body (streams/open-input-stream (:file resource))))
    [:http.status/not-found]))

(defn http-store []
  (collaj/create-store reducers/http))

(defn ws-store []
  (collaj/create-store reducers/ws collaj.enhancers/with-fn-dispatch))

(defn file-store []
  (collaj/create-store reducers/http))

(def delay (comp :delay :current :config))

(defn response [state]
  (-> state
      (get-in [:config :current :response])
      (maps/update-maybe :headers (partial maps/map-keys name))))

(defn file-response [env state]
  (-> state
      (response)
      (with-file env)))

(def requests :requests)

(defn details [state]
  (-> state
      (select-keys #{:config :requests :sockets})
      (update :config :current)
      (maps/update-maybe :sockets (comp set keys #(maps/dissocp % nil?)))))
