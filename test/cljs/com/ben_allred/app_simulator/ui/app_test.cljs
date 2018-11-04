(ns com.ben-allred.app-simulator.ui.app-test
  (:require
    [clojure.test :as t :refer-macros [deftest is testing]]
    [com.ben-allred.app-simulator.templates.views.core :as views]
    [com.ben-allred.app-simulator.ui.app :as app]
    [com.ben-allred.app-simulator.ui.services.store.core :as store]
    [com.ben-allred.app-simulator.ui.views.components.modal :as modal]
    [com.ben-allred.app-simulator.ui.views.components.toast :as toast]
    [com.ben-allred.app-simulator.ui.views.main :as main]
    [test.utils.spies :as spies]))

(defn ^:private state []
  {:page             {:handler :component}
   :toasts           ::toasts
   :something-random (rand-int 1000)
   :modal            ::modal-data})

(deftest ^:unit app-test
  (testing "(app)"
    (with-redefs [store/get-state (spies/constantly ::state)
                  store/dispatch (constantly nil)
                  toast/toast (spies/constantly ::toast)
                  modal/modal (spies/constantly ::modal)]
      (let [app (app/app)]
        (testing "when rendering the tree"
          (spies/reset! store/get-state)
          (let [[component attrs state] (app)]
            (testing "uses views/app*"
              (is (spies/called-with? store/get-state))
              (is (= state ::state))
              (is (= component views/app*)))

            (testing "has components"
              (is (= main/root (get-in attrs [:components :home])))
              (is (= main/new (get-in attrs [:components :new])))
              (is (= main/details (get-in attrs [:components :details])))
              (is (= main/resources (get-in attrs [:components :resources]))))

            (testing "has a toast"
              (let [toast ((:toast attrs) {:toasts ::toasts})]
                (is (= toast ::toast))
                (is (spies/called-with? toast/toast ::toasts))))

            (testing "has a modal"
              (let [modal ((:modal attrs) {:modal ::modal-data})]
                (is (= modal ::modal))
                (is (spies/called-with? modal/modal ::modal-data))))))))))

(defn run-tests []
  (t/run-tests))
