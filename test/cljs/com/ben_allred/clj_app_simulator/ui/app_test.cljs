(ns ^:figwheel-load com.ben-allred.clj-app-simulator.ui.app-test
  (:require [cljs.test :as t :refer-macros [deftest testing is]]
            [com.ben-allred.clj-app-simulator.ui.app :as app]
            [com.ben-allred.clj-app-simulator.utils.keywords :as keywords]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [test.utils.dom :as test.dom]
            [com.ben-allred.clj-app-simulator.ui.views.components.modal :as modal]
            [com.ben-allred.clj-app-simulator.ui.views.components.toast :as toast]
            [com.ben-allred.clj-app-simulator.ui.views.main :as main]))

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
          get-state-spy (spies/create (constantly state))]
      (with-redefs [app/components components
                    store/get-state get-state-spy]
        (testing "gets state from store"
          (spies/reset! get-state-spy)
          (let [app (app/app)]
            (is (spies/called-with? get-state-spy))))
        (testing "mounts modal with state"
          (let [app (app/app)
                modal (test.dom/query-one app modal/modal)]
            (is (= (second modal) ::modal-data))))
        (testing "mounts toast with toasts from state"
          (let [app (app/app)
                toast (test.dom/query-one app toast/toast)]
            (is (= (second toast) ::toasts))))
        (testing "mounts header"
          (is (test.dom/query-one (app/app) main/header)))
        (testing "mounts component with state"
          (let [app (app/app)
                comp (test.dom/query-one app (:component components))]
            (is (= (second comp) state))))))))

(defn run-tests [] (t/run-tests))
