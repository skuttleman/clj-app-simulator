(ns com.ben-allred.clj-app-simulator.ui.services.store.core
  (:require
    [com.ben-allred.clj-app-simulator.services.ui-reducers :as reducers]
    [com.ben-allred.clj-app-simulator.ui.services.store.activity :as activity]
    [com.ben-allred.clj-app-simulator.ui.services.store.middleware :as mw]
    [com.ben-allred.clj-app-simulator.utils.logging :as log :include-macros true]
    [com.ben-allred.collaj.core :as collaj]
    [com.ben-allred.collaj.enhancers :as collaj.enhancers]
    [reagent.core :as r]))

(defonce ^:private store
  (activity/sub
    (collaj/create-custom-store
      r/atom
      reducers/root
      collaj.enhancers/with-fn-dispatch
      (collaj/apply-middleware mw/sims->sim)
      (collaj.enhancers/with-log-middleware
        (partial js/console.log "Action dispatched:")
        (partial js/console.log "New state:")))))

(def get-state (:get-state store))

(def dispatch (:dispatch store))
