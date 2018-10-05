(ns com.ben-allred.clj-app-simulator.ui.app-test
  (:require [clojure.test :as t :refer-macros [deftest testing is]]
            [com.ben-allred.clj-app-simulator.ui.app :as app]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.views.components.modal :as modal]
            [com.ben-allred.clj-app-simulator.ui.views.components.toast :as toast]
            [com.ben-allred.clj-app-simulator.ui.views.main :as main]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.templates.views.core :as views]))

(defn ^:private state []
  {:page             {:handler :component}
   :toasts           ::toasts
   :something-random (rand-int 1000)
   :modal            ::modal-data})

(deftest ^:unit app-test
  (testing "(app)"
    (let [get-state-spy (spies/constantly ::state)
          dispatch-spy (spies/create)
          toast-spy (spies/constantly ::toast)
          modal-spy (spies/constantly ::modal)]
      (with-redefs [store/get-state get-state-spy
                    store/dispatch dispatch-spy
                    toast/toast toast-spy
                    modal/modal modal-spy]
        (let [app (app/app)]
          (testing "when rendering the tree"
            (spies/reset! get-state-spy)
            (let [[component attrs state] (app)]
              (testing "uses views/app*"
                (is (spies/called-with? get-state-spy))
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
                  (is (spies/called-with? toast-spy ::toasts))))

              (testing "has a modal"
                (let [modal ((:modal attrs) {:modal ::modal-data})]
                  (is (= modal ::modal))
                  (is (spies/called-with? modal-spy ::modal-data)))))))))))

(defn run-tests []
  (t/run-tests))
