(ns com.ben-allred.clj-app-simulator.ui.views.components.toast-test
  (:require [clojure.test :as t :refer-macros [deftest testing is]]
            [com.ben-allred.clj-app-simulator.ui.views.components.toast :as toast]
            [test.utils.dom :as test.dom]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]))

(deftest ^:unit toast-test
  (testing "(toast)"
    (let [toast (toast/toast {:key-3 {:text  "Toast 3"
                                      :level :success}
                              :key-1 {:text  "Toast 1"
                                      :level :success}
                              :key-2 {:text  "Toast 2"
                                      :level :error}})]
      (testing "has sorted toast messages"
        (let [[toast-1 toast-2 toast-3 :as toast-messages] (test.dom/query-all toast :.toast-message)]
          (is (= 3 (count toast-messages)))
          (test.dom/contains? toast-1 "Toast 1")
          (test.dom/contains? toast-2 "Toast 2")
          (test.dom/contains? toast-3 "Toast 3")))

      (testing "adds class for message level"
        (is (= 1 (count (test.dom/query-all toast :.toast-message.is-danger))))
        (is (= 2 (count (test.dom/query-all toast :.toast-message.is-success)))))

      (doseq [[idx key] [[0 :key-1] [1 :key-2] [2 :key-3]]]
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
