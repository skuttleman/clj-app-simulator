(ns com.ben-allred.clj-app-simulator.ui.app
  (:require [cljs.core.async :as async]
            [cljsjs.moment]
            [com.ben-allred.clj-app-simulator.templates.views.core :as views]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.views.components.modal :as modal]
            [com.ben-allred.clj-app-simulator.ui.views.components.toast :as toast]
            [com.ben-allred.clj-app-simulator.ui.views.main :as main]
            [com.ben-allred.clj-app-simulator.utils.logging :as log :include-macros true]
            [reagent.core :as r]))

(enable-console-print!)

(defn ^:private attrs []
  {:components {:home      main/root
                :new       main/new
                :details   main/details
                :resources main/resources}
   :toast      (comp toast/toast :toasts)
   :modal      (comp modal/modal :modal)})

(defn app []
  (let [attrs (attrs)]
    (fn []
      [views/app* attrs (store/get-state)])))

(defn ^:export mount! []
  (async/go
    (let [uploads (store/dispatch actions/get-uploads)]
      (async/<! (store/dispatch actions/request-simulators))
      (async/<! uploads)
      (r/render
        [app]
        (.getElementById js/document "app")))))
