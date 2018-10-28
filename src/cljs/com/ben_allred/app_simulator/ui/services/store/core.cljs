(ns com.ben-allred.app-simulator.ui.services.store.core
  (:require
    [com.ben-allred.app-simulator.services.env :as env]
    [com.ben-allred.app-simulator.services.ui-reducers :as reducers]
    [com.ben-allred.app-simulator.ui.services.store.activity :as activity]
    [com.ben-allred.app-simulator.ui.services.store.middleware :as mw]
    [com.ben-allred.collaj.core :as collaj]
    [com.ben-allred.collaj.enhancers :as collaj.enhancers]
    [reagent.core :as r]))

(defonce ^:private store
  (activity/sub
    (apply collaj/create-custom-store
           r/atom
           reducers/root
           collaj.enhancers/with-fn-dispatch
           (collaj/apply-middleware mw/sims->sim)
           (cond-> nil
             (env/get :dev?) (conj (collaj.enhancers/with-log-middleware
                                     (partial js/console.log "Action dispatched:")
                                     (partial js/console.log "New state:")))))))

(def get-state (:get-state store))

(def dispatch (:dispatch store))
