(ns com.ben-allred.clj-app-simulator.ui.app
  (:require [cljsjs.moment]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.views.components.modal :as modal]
            [com.ben-allred.clj-app-simulator.ui.views.components.toast :as toast]
            [com.ben-allred.clj-app-simulator.ui.views.error :as error]
            [com.ben-allred.clj-app-simulator.ui.views.main :as main]
            [com.ben-allred.clj-app-simulator.utils.logging :as log :include-macros true]
            [reagent.core :as r]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]))

(enable-console-print!)

(def ^:private components
  {:home    main/root
   :new     main/new
   :details main/details
   :resources main/resources})

(defn app []
  (store/dispatch actions/get-uploads)
  (store/dispatch actions/request-simulators)
  (fn []
    (let [{:keys [page] :as state} (store/get-state)
          component (components (:handler page) error/not-found)]
      [:div.app
       [toast/toast (:toasts state)]
       [:div.scrollable
        [main/header page]
        [:main.main
         [component state]]]
       [modal/modal (:modal state)]])))

(defn ^:export mount! []
  (r/render
    [app]
    (.getElementById js/document "app")))
