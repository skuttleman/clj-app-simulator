(ns com.ben-allred.clj-app-simulator.ui.app
  (:require [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.views.error :as error]
            [com.ben-allred.clj-app-simulator.ui.views.components.toast :as toast]
            [com.ben-allred.clj-app-simulator.utils.logging :as log :include-macros true]
            [com.ben-allred.clj-app-simulator.ui.views.components.modal :as modal]
            [reagent.core :as r]
            [com.ben-allred.clj-app-simulator.ui.views.main :as main]))

(enable-console-print!)

(def components
  {:home    main/root
   :details (fn [state]
              [:div
               "Details"
               (pr-str (get-in state [:simulators :data (uuid (get-in state [:page :route-params :id]))]))])})

(defn app []
  (let [state (store/get-state)
        component (components (get-in state [:page :handler]) error/not-found)]
    [:div.app
     [modal/modal (:modal state)]
     [toast/toast (:toasts state)]
     [:div.scrollable
      [main/header]
      [:main.main
       [component state]]]]))

(defn mount! []
  (r/render
    [app]
    (.getElementById js/document "app")))

#_(defn on-js-reload []
    (mount!))
