(ns com.ben-allred.app-simulator.ui.views.components.toast-test
  (:require
    [clojure.test :as t :refer-macros [deftest is testing]]
    [com.ben-allred.app-simulator.ui.services.store.actions :as actions]
    [com.ben-allred.app-simulator.ui.services.store.core :as store]
    [com.ben-allred.app-simulator.ui.views.components.toast :as toast]
    [test.utils.dom :as test.dom]
    [test.utils.spies :as spies]))

(deftest ^:unit toast-test
  (testing "(toast)"
    (let [toast (toast/toast {:key-3 {:ref   (atom "Toast 3")
                                      :level :success}
                              :key-1 {:ref   (atom "Toast 1")
                                      :level :success}
                              :key-2 {:ref   (atom "Toast 2")
                                      :level :error}})]
      (let [[toast-1 toast-2 toast-3 :as toast-messages] (test.dom/query-all toast :.toast-message)]
        (testing "limits display of toasts"
          (is (= 2 (count toast-messages)))
          (is (not (test.dom/contains? toast-3 "Toast 3"))))

        (testing "has sorted toast messages"
          (is (test.dom/contains? toast-1 "Toast 1"))
          (is (test.dom/contains? toast-2 "Toast 2"))))

      (testing "adds class for message level"
        (is (= 1 (count (test.dom/query-all toast :.toast-message.is-danger))))
        (is (= 1 (count (test.dom/query-all toast :.toast-message.is-success)))))

      (doseq [[idx key] [[0 :key-1] [1 :key-2]]]
        (testing (str "has remove button for toast " key)
          (let [toast-message (nth (test.dom/query-all toast :.toast-message) idx)
                button (test.dom/query-one toast-message :.delete)
                dispatch-spy (spies/create)
                action-spy (spies/constantly ::action)]
            (with-redefs [store/dispatch dispatch-spy
                          actions/remove-toast action-spy]
              (test.dom/simulate-event button :click)
              (is (spies/called-with? action-spy key))
              (is (spies/called-with? dispatch-spy ::action)))))))))

(defn run-tests []
  (t/run-tests))
