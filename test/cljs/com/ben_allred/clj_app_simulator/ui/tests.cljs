(ns ^:figwheel-always com.ben-allred.clj-app-simulator.ui.tests
  (:require [cljs.test :refer-macros [run-all-tests]]
            [com.ben-allred.clj-app-simulator.ui.app :as app]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions-test]
            [com.ben-allred.clj-app-simulator.ui.services.store.reducers-test]
            [com.ben-allred.clj-app-simulator.ui.services.events-test]
            [com.ben-allred.clj-app-simulator.ui.services.navigation-test]
            [com.ben-allred.clj-app-simulator.ui.services.store.activity-test]
            [com.ben-allred.clj-app-simulator.ui.services.store.middleware-test]
            [com.ben-allred.clj-app-simulator.ui.utils.core-test]
            [com.ben-allred.clj-app-simulator.ui.utils.macros-test]
            [com.ben-allred.clj-app-simulator.ui.views.components.core-test]
            [com.ben-allred.clj-app-simulator.ui.views.components.modal-test]
            [com.ben-allred.clj-app-simulator.ui.views.components.toast-test]
            [com.ben-allred.clj-app-simulator.ui.views.error-test]
            [com.ben-allred.clj-app-simulator.ui.views.main-test]
            [com.ben-allred.clj-app-simulator.ui.views.simulator-test]
            [com.ben-allred.clj-app-simulator.ui.views.simulators-test]
            [com.ben-allred.clj-app-simulator.ui.app-test]))

(enable-console-print!)

(defn ^:export run-tests []
  (run-all-tests
    #"com\.ben-allred\.clj-app-simulator\.ui.*-test"))

(defn ^:export on-reload []
  ;(run-tests)
  (app/mount!))
