(ns com.ben-allred.clj-app-simulator.api.services.simulators.store.core
  (:refer-clojure :exclude [delay])
  (:require [com.ben-allred.collaj.core :as collaj]
            [com.ben-allred.clj-app-simulator.api.services.simulators.store.reducers :as reducers]
            [com.ben-allred.clj-app-simulator.utils.maps :as maps]))

(defn http-store [] (collaj/create-store reducers/http))

(def delay (comp :delay :current :config))

(defn response [state]
  (-> state
      (:config)
      (:current)
      (:response)
      (maps/update-maybe :headers (partial maps/map-keys name))))

(def requests :requests)

(defn details [state]
  (-> state
      (select-keys #{:config :requests})
      (update :config :current)))
