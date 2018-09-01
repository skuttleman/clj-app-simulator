(ns com.ben-allred.clj-app-simulator.ui.app
  (:require [cljsjs.moment]
            [com.ben-allred.clj-app-simulator.templates.views.core :as views]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
            [com.ben-allred.clj-app-simulator.ui.views.components.modal :as modal]
            [com.ben-allred.clj-app-simulator.ui.views.components.toast :as toast]
            [com.ben-allred.clj-app-simulator.ui.views.main :as main]
            [com.ben-allred.clj-app-simulator.utils.logging :as log :include-macros true]
            [reagent.core :as r]))

(enable-console-print!)

(defn ^:private attrs []
  {:header     (comp main/header :page)
   :components {:home      main/root
                :new       main/new
                :details   main/details
                :resources main/resources}
   :not-found  main/not-found
   :toast      (comp toast/toast :toasts)
   :modal      (comp modal/modal :modal)})

(defn app []
  (store/dispatch actions/get-uploads)
  (store/dispatch actions/request-simulators)
  (let [attrs (attrs)]
    (fn []
      [views/app* attrs (store/get-state)])))

(defn ^:export mount! []
  (r/render
    [app]
    (.getElementById js/document "app")))
