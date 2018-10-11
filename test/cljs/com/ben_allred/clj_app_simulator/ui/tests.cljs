(ns com.ben-allred.clj-app-simulator.ui.tests
  (:require [clojure.test :as t :include-macros true]
            [com.ben-allred.clj-app-simulator.services.content-test :as content-test]
            [com.ben-allred.clj-app-simulator.services.emitter-test :as emitter-test]
            [com.ben-allred.clj-app-simulator.services.http-test :as http-test]
            [com.ben-allred.clj-app-simulator.services.navigation-test :as navigation*-test]
            [com.ben-allred.clj-app-simulator.services.ui-reducers-test :as ui-reducers-test]
            [com.ben-allred.clj-app-simulator.services.ws-test :as ws-test]
            [com.ben-allred.clj-app-simulator.templates.core-test :as core-test]
            [com.ben-allred.clj-app-simulator.templates.fields-test :as fields-test]
            [com.ben-allred.clj-app-simulator.templates.resources.file-test :as resources.file-test]
            [com.ben-allred.clj-app-simulator.templates.resources.http-test :as resources.http-test]
            [com.ben-allred.clj-app-simulator.templates.resources.ws-test :as resources.ws-test]
            [com.ben-allred.clj-app-simulator.templates.transformations.file-test :as transformations.file-test]
            [com.ben-allred.clj-app-simulator.templates.transformations.http-test :as transformations.http-test]
            [com.ben-allred.clj-app-simulator.templates.transformations.shared-test :as transformations.shared-test]
            [com.ben-allred.clj-app-simulator.templates.transformations.ws-test :as transformations.ws-test]
            [com.ben-allred.clj-app-simulator.templates.views.core-test :as views.core-test]
            [com.ben-allred.clj-app-simulator.templates.views.forms.file-test :as views.forms.file-test]
            [com.ben-allred.clj-app-simulator.templates.views.forms.http-test :as views.forms.http-test]
            [com.ben-allred.clj-app-simulator.templates.views.forms.shared-test :as views.forms.shared-test]
            [com.ben-allred.clj-app-simulator.templates.views.forms.ws-test :as views.forms.ws-test]
            [com.ben-allred.clj-app-simulator.templates.views.simulators-test :as views.simulators-test]
            [com.ben-allred.clj-app-simulator.utils.colls-test :as colls-test]
            [com.ben-allred.clj-app-simulator.utils.keywords-test :as keywords-test]
            [com.ben-allred.clj-app-simulator.utils.maps-test :as maps-test]
            [com.ben-allred.clj-app-simulator.utils.query-params-test :as query-params-test]
            [com.ben-allred.clj-app-simulator.utils.strings-test :as strings-test]
            [com.ben-allred.clj-app-simulator.ui.app :as app]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions-test :as actions-test]
            [com.ben-allred.clj-app-simulator.ui.services.events-test :as events-test]
            [com.ben-allred.clj-app-simulator.ui.services.forms.core-test :as forms.core-test]
            [com.ben-allred.clj-app-simulator.ui.services.navigation-test :as navigation-test]
            [com.ben-allred.clj-app-simulator.ui.services.store.activity-test :as activity-test]
            [com.ben-allred.clj-app-simulator.ui.services.store.middleware-test :as middleware-test]
            [com.ben-allred.clj-app-simulator.ui.simulators.file.interactions-test :as file.interactions-test]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.interactions-test :as http.interactions-test]
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
  (content-test/run-tests)
  (emitter-test/run-tests)
  (http-test/run-tests)
  (navigation*-test/run-tests)
  (ui-reducers-test/run-tests)
  (ws-test/run-tests)
  (core-test/run-tests)
  (fields-test/run-tests)
  (resources.file-test/run-tests)
  (resources.http-test/run-tests)
  (resources.ws-test/run-tests)
  (transformations.file-test/run-tests)
  (transformations.http-test/run-tests)
  (transformations.shared-test/run-tests)
  (transformations.ws-test/run-tests)
  (views.core-test/run-tests)
  (views.forms.file-test/run-tests)
  (views.forms.http-test/run-tests)
  (views.forms.shared-test/run-tests)
  (views.forms.ws-test/run-tests)
  (views.simulators-test/run-tests)
  (colls-test/run-tests)
  (keywords-test/run-tests)
  (maps-test/run-tests)
  (query-params-test/run-tests)
  (strings-test/run-tests)
  (actions-test/run-tests)
  (events-test/run-tests)
  (forms.core-test/run-tests)
  (navigation-test/run-tests)
  (activity-test/run-tests)
  (middleware-test/run-tests)
  (file.interactions-test/run-tests)
  (http.interactions-test/run-tests)
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
