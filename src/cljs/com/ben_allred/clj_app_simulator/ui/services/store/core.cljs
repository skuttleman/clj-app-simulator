(ns com.ben-allred.clj-app-simulator.ui.services.store.core
    (:require [com.ben-allred.collaj.core :as collaj]
              [com.ben-allred.collaj.enhancers :as collaj.enhancers]
              [com.ben-allred.clj-app-simulator.ui.services.store.reducers :as reducers]
              [reagent.core :as r]
              [com.ben-allred.clj-app-simulator.utils.logging :as log :include-macros true]))

(defonce ^:private store (collaj/create-custom-store r/atom
                         reducers/root
                         collaj.enhancers/with-fn-dispatch
                         (collaj.enhancers/with-log-middleware #(log/spy %) identity)))

(def get-state (:get-state store))

(def dispatch (:dispatch store))
