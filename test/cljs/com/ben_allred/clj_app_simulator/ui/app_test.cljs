(ns com.ben-allred.clj-app-simulator.ui.app-test
  (:require [cljs.test :as t :refer-macros [deftest testing is]]
            [com.ben-allred.clj-app-simulator.ui.app :as app]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.views.components.modal :as modal]
            [com.ben-allred.clj-app-simulator.ui.views.components.toast :as toast]
            [com.ben-allred.clj-app-simulator.ui.views.main :as main]
            [com.ben-allred.clj-app-simulator.utils.keywords :as keywords]
            [test.utils.dom :as test.dom]
            [test.utils.spies :as spies]))

(defn ^:private components []
  {:component (keywords/join [:component.class- (str (rand-int 1000))])})

(defn ^:private state []
  {:page             {:handler :component}
   :toasts           ::toasts
   :something-random (rand-int 1000)
   :modal            ::modal-data})

(deftest ^:unit app-test
  (testing "(app-test)"
    (let [state (state)
          components (components)
          get-state-spy (spies/constantly state)
          dispatch-spy (spies/create)]
      (with-redefs [app/components components
                    store/get-state get-state-spy
                    store/dispatch dispatch-spy]
        (let [root (app/app)]
          (testing "dispatches on mount"
            (is (spies/called-with? dispatch-spy actions/get-uploads))
            (is (spies/called-with? dispatch-spy actions/request-simulators))

            (testing "gets state from store"
              (spies/reset! get-state-spy)
              (root)
              (is (spies/called-with? get-state-spy)))

            (testing "mounts modal with state"
              (let [app (root)
                    modal (test.dom/query-one app modal/modal)]
                (is (= (second modal) ::modal-data))))

            (testing "mounts toast with toasts from state"
              (let [app (root)
                    toast (test.dom/query-one app toast/toast)]
                (is (= (second toast) ::toasts))))

            (testing "mounts header"
              (is (test.dom/query-one (root) main/header)))

            (testing "mounts component with state"
              (let [app (root)
                    comp (test.dom/query-one app (:component components))]
                (is (= (second comp) state))))))))))

(defn run-tests []
  (t/run-tests))
