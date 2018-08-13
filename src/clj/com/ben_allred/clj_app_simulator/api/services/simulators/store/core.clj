(ns com.ben-allred.clj-app-simulator.api.services.simulators.store.core
  (:refer-clojure :exclude [delay])
  (:require [com.ben-allred.clj-app-simulator.api.services.resources.core :as resources]
            [com.ben-allred.clj-app-simulator.api.services.simulators.store.reducers :as reducers]
            [com.ben-allred.clj-app-simulator.api.services.streams :as streams]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.utils.maps :as maps]
            [com.ben-allred.collaj.core :as collaj]
            [com.ben-allred.collaj.enhancers :as collaj.enhancers]))

(defn ^:private with-file [response]
  (if-let [file (resources/get (:file response))]
    (-> response
        (dissoc :file)
        (update :headers assoc
                "Content-Type" (:content-type file)
                "Content-Length" (:size file)
                "Content-Disposition" (format "inline; filename=\"%s\"" (:filename file)))
        (assoc :body (streams/open-input-stream (:file file))))
    [:not-found]))

(defn http-store []
  (collaj/create-store reducers/http))

(defn ws-store []
  (collaj/create-store reducers/ws collaj.enhancers/with-fn-dispatch))

(defn file-store []
  (collaj/create-store reducers/http))

(def delay (comp :delay :current :config))

(defn response [state]
  (-> state
      (:config)
      (:current)
      (:response)
      (maps/update-maybe :headers (partial maps/map-keys name))))

(defn file-response [state]
  (-> state
      (:config)
      (:current)
      (:response)
      (update :headers (partial maps/map-keys name))
      (with-file)))

(def requests :requests)

(defn details [state]
  (-> state
      (select-keys #{:config :requests :sockets})
      (update :config :current)
      (maps/update-maybe :sockets (comp set keys #(maps/dissocp % nil?)))))
