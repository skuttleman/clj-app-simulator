(ns com.ben-allred.clj-app-simulator.ui.tests
  (:require [com.ben-allred.clj-app-simulator.services.forms-test :as forms-test]
            [com.ben-allred.clj-app-simulator.templates.components.core-test :as templates-test]
            [com.ben-allred.clj-app-simulator.templates.components.form-fields-test :as ff-test]
            [com.ben-allred.clj-app-simulator.templates.simulators.file.views-test :as file.views-test]
            [com.ben-allred.clj-app-simulator.templates.simulators.http.views-test :as http.views-test]
            [com.ben-allred.clj-app-simulator.templates.simulators.shared.views-test :as shared.views-test]
            [com.ben-allred.clj-app-simulator.templates.simulators.ws.views-test :as ws.views-test]
            [com.ben-allred.clj-app-simulator.templates.views.error-test :as error-test]
            [com.ben-allred.clj-app-simulator.templates.views.main-test :as main-test]
            [com.ben-allred.clj-app-simulator.templates.views.simulators-test :as simulators-test]
            [com.ben-allred.clj-app-simulator.ui.app :as app]
            [com.ben-allred.clj-app-simulator.ui.app-test :as app-test]
            [com.ben-allred.clj-app-simulator.ui.services.events-test :as events-test]
            [com.ben-allred.clj-app-simulator.ui.services.navigation-test :as navigation-test]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions-test :as actions-test]
            [com.ben-allred.clj-app-simulator.ui.services.store.activity-test :as activity-test]
            [com.ben-allred.clj-app-simulator.ui.services.store.middleware-test :as middleware-test]
            [com.ben-allred.clj-app-simulator.ui.simulators.file.interactions-test :as file.interactions-test]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.interactions-test :as http.interactions-test]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions-test :as shared.interactions-test]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.modals-test :as shared.modals-test]
            [com.ben-allred.clj-app-simulator.ui.simulators.ws.interactions-test :as ws.interactions-test]
            [com.ben-allred.clj-app-simulator.ui.simulators.ws.modals-test :as ws.modals-test]
            [com.ben-allred.clj-app-simulator.ui.utils.macros-test :as macros-test]
            [com.ben-allred.clj-app-simulator.ui.views.components.modal-test :as modal-test]
            [com.ben-allred.clj-app-simulator.ui.views.components.toast-test :as toast-test]))

(enable-console-print!)

(defn on-reload []
  (app/mount!))

(defn run-tests []
  (forms-test/run-tests)
  (templates-test/run-tests)
  (ff-test/run-tests)
  (file.views-test/run-tests)
  (http.views-test/run-tests)
  (shared.views-test/run-tests)
  (ws.views-test/run-tests)
  (error-test/run-tests)
  (main-test/run-tests)
  (simulators-test/run-tests)
  (app-test/run-tests)
  (events-test/run-tests)
  (navigation-test/run-tests)
  (actions-test/run-tests)
  (activity-test/run-tests)
  (middleware-test/run-tests)
  (file.interactions-test/run-tests)
  (http.interactions-test/run-tests)
  (shared.interactions-test/run-tests)
  (shared.modals-test/run-tests)
  (ws.interactions-test/run-tests)
  (ws.modals-test/run-tests)
  (macros-test/run-tests)
  (modal-test/run-tests)
  (toast-test/run-tests))

(comment
  (with-out-str
    (run-tests)))
