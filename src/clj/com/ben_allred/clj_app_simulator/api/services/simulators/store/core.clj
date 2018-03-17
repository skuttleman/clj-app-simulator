(ns com.ben-allred.clj-app-simulator.api.services.simulators.store.core
    (:refer-clojure :exclude [delay])
    (:require [com.ben-allred.collaj.core :as collaj]
              [com.ben-allred.clj-app-simulator.api.services.simulators.store.reducers :as reducers]))

(defn http-store [] (collaj/create-store reducers/http))

(def delay (comp :delay :current :config))

(def response (comp :response :current :config))

(def requests :requests)

(def config (comp :current :config))
