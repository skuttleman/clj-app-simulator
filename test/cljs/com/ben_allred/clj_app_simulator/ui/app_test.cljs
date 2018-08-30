(ns com.ben-allred.clj-app-simulator.ui.app-test
  (:require [clojure.test :as t :refer-macros [deftest testing is]]
            [com.ben-allred.clj-app-simulator.ui.app :as app]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.templates.views.main :as main]
            [test.utils.spies :as spies]))

(defn ^:private state []
  {:page             {:handler :component}
   :toasts           ::toasts
   :something-random (rand-int 1000)
   :modal            ::modal-data})

(deftest ^:unit app-test
  (testing "(app-test)"
    (let [state (state)
          get-state-spy (spies/constantly state)
          dispatch-spy (spies/create)]
      (with-redefs [store/get-state get-state-spy
                    store/dispatch dispatch-spy]
        (let [root (app/app)]
          (testing "dispatches on mount"
            (is (spies/called-with? dispatch-spy actions/get-uploads))
            (is (spies/called-with? dispatch-spy actions/request-simulators))

            (testing "gets state from store"
              (spies/reset! get-state-spy)
              (let [[component arg] (root)]
                (is (spies/called-with? get-state-spy))
                (is (= component main/app))
                (is (= arg state))))))))))

(defn run-tests []
  (t/run-tests))
