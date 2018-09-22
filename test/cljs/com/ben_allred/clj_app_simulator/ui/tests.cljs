(ns com.ben-allred.clj-app-simulator.ui.tests
  (:require [cljs.test :as t :include-macros true]
            [com.ben-allred.clj-app-simulator.ui.app :as app]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions-test :as actions-test]
            [com.ben-allred.clj-app-simulator.ui.services.events-test :as events-test]
            [com.ben-allred.clj-app-simulator.ui.services.forms.core-test :as forms.core-test]
            [com.ben-allred.clj-app-simulator.ui.services.navigation-test :as navigation-test]
            [com.ben-allred.clj-app-simulator.ui.services.store.activity-test :as activity-test]
            [com.ben-allred.clj-app-simulator.ui.services.store.middleware-test :as middleware-test]
            [com.ben-allred.clj-app-simulator.ui.simulators.file.interactions-test :as file.interactions-test]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.interactions-test :as http.interactions-test]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.modals-test :as http.modals-test]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions-test :as shared.interactions-test]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.modals-test :as shared.modals-test]
            [com.ben-allred.clj-app-simulator.ui.simulators.ws.interactions-test :as ws.interactions-test]
            [com.ben-allred.clj-app-simulator.ui.utils.macros-test :as macros-test]
            [com.ben-allred.clj-app-simulator.ui.views.components.core-test :as components.core-test]
            [com.ben-allred.clj-app-simulator.ui.views.components.modal-test :as modal-test]
            [com.ben-allred.clj-app-simulator.ui.views.components.toast-test :as toast-test]
            [com.ben-allred.clj-app-simulator.ui.views.main-test :as main-test]
            [com.ben-allred.clj-app-simulator.ui.views.resources-test :as resources-test]
            [com.ben-allred.clj-app-simulator.ui.app-test :as app-test]))

(enable-console-print!)

(defn on-reload []
  (app/mount!))

(defn run-tests []
  (actions-test/run-tests)
  (events-test/run-tests)
  (forms.core-test/run-tests)
  (navigation-test/run-tests)
  (activity-test/run-tests)
  (middleware-test/run-tests)
  (file.interactions-test/run-tests)
  (http.interactions-test/run-tests)
  (http.modals-test/run-tests)
  (shared.interactions-test/run-tests)
  (shared.modals-test/run-tests)
  (ws.interactions-test/run-tests)
  (macros-test/run-tests)
  (components.core-test/run-tests)
  (modal-test/run-tests)
  (toast-test/run-tests)
  (main-test/run-tests)
  (resources-test/run-tests)
  (app-test/run-tests))
