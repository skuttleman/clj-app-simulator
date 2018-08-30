(ns com.ben-allred.clj-app-simulator.ui.app
  (:require [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.templates.views.main :as main]
            [com.ben-allred.clj-app-simulator.utils.logging :as log :include-macros true]
            [reagent.core :as r]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]))

(enable-console-print!)

(defn app []
  (store/dispatch actions/get-uploads)
  (store/dispatch actions/request-simulators)
  (fn []
    [main/app (store/get-state)]))

(defn ^:export mount! []
  (r/render
    [app]
    (.getElementById js/document "app")))
