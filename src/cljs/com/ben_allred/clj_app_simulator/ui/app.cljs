(ns com.ben-allred.clj-app-simulator.ui.app
    (:require [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
              [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
              [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
              [com.ben-allred.clj-app-simulator.services.http :as http]
              [com.ben-allred.clj-app-simulator.ui.views.components.core :as components]
              [com.ben-allred.clj-app-simulator.ui.views.error :as error]
              [com.ben-allred.clj-app-simulator.ui.views.home :as home]
              [com.ben-allred.clj-app-simulator.ui.views.components.toast :as toast]
              [com.ben-allred.clj-app-simulator.services.env :as env]
              [com.ben-allred.clj-app-simulator.utils.logging :as log :include-macros true]
              [com.ben-allred.clj-app-simulator.ui.views.components.modal :as modal]
              [reagent.core :as r]
              [com.ben-allred.clj-app-simulator.ui.views.main :as main]))

(enable-console-print!)

(defn on-js-reload [])

(def components
    {:home  home/root})

(defn app []
    (store/dispatch actions/request-user-details)
    (fn []
        (let [state (store/get-state)
              component (components (get-in state [:page :handler]) error/not-found)]
            [:div.app
             [modal/modal state]
             [toast/toast (:toasts state)]
             [:div.scrollable
              [main/header true]
              [:main.main
               [component state]]]])))

(r/render
    [app]
    (.getElementById js/document "app"))
