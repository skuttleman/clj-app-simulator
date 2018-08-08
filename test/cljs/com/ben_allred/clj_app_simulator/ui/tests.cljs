(ns com.ben-allred.clj-app-simulator.ui.tests
  (:require [cljs.test :as t :include-macros true]
            [com.ben-allred.clj-app-simulator.ui.app :as app]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions-test :as actions-test]
            [com.ben-allred.clj-app-simulator.ui.services.store.reducers-test :as reducers-test]
            [com.ben-allred.clj-app-simulator.ui.services.events-test :as events-test]
            [com.ben-allred.clj-app-simulator.ui.services.forms.fields-test :as fields-test]
            [com.ben-allred.clj-app-simulator.ui.services.forms.core-test :as forms.core-test]
            [com.ben-allred.clj-app-simulator.ui.services.navigation-test :as navigation-test]
            [com.ben-allred.clj-app-simulator.ui.services.store.activity-test :as activity-test]
            [com.ben-allred.clj-app-simulator.ui.services.store.middleware-test :as middleware-test]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.interactions-test :as http.interactions-test]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.modals-test :as http.modals-test]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.resources-test :as http.resources-test]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.transformations-test :as http.transformations-test]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.views-test :as http.views-test]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions-test :as shared.interactions-test]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.modals-test :as shared.modals-test]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.views-test :as shared.views-test]
            [com.ben-allred.clj-app-simulator.ui.simulators.ws.interactions-test :as ws.interactions-test]
            [com.ben-allred.clj-app-simulator.ui.simulators.ws.resources-test :as ws.resources-test]
            [com.ben-allred.clj-app-simulator.ui.simulators.ws.transformations-test :as ws.transformations-test]
            [com.ben-allred.clj-app-simulator.ui.simulators.ws.views-test :as ws.views-test]
            [com.ben-allred.clj-app-simulator.ui.utils.core-test :as utils.core-test]
            [com.ben-allred.clj-app-simulator.ui.utils.macros-test :as macros-test]
            [com.ben-allred.clj-app-simulator.ui.views.components.core-test :as components.core-test]
            [com.ben-allred.clj-app-simulator.ui.views.components.modal-test :as modal-test]
            [com.ben-allred.clj-app-simulator.ui.views.components.toast-test :as toast-test]
            [com.ben-allred.clj-app-simulator.ui.views.error-test :as error-test]
            [com.ben-allred.clj-app-simulator.ui.views.main-test :as main-test]
            [com.ben-allred.clj-app-simulator.ui.views.simulators-test :as simulators-test]
            [com.ben-allred.clj-app-simulator.ui.app-test :as app-test]))

(enable-console-print!)

(defn on-reload []
  (app/mount!))

(defn run-tests []
  (actions-test/run-tests)
  (reducers-test/run-tests)
  (events-test/run-tests)
  (fields-test/run-tests)
  (forms.core-test/run-tests)
  (navigation-test/run-tests)
  (activity-test/run-tests)
  (middleware-test/run-tests)
  (http.interactions-test/run-tests)
  (http.modals-test/run-tests)
  (http.resources-test/run-tests)
  (http.transformations-test/run-tests)
  (http.views-test/run-tests)
  (shared.interactions-test/run-tests)
  (shared.modals-test/run-tests)
  (shared.views-test/run-tests)
  (ws.interactions-test/run-tests)
  (ws.resources-test/run-tests)
  (ws.transformations-test/run-tests)
  (ws.views-test/run-tests)
  (utils.core-test/run-tests)
  (macros-test/run-tests)
  (components.core-test/run-tests)
  (modal-test/run-tests)
  (toast-test/run-tests)
  (error-test/run-tests)
  (main-test/run-tests)
  (simulators-test/run-tests)
  (app-test/run-tests))
