(ns com.ben-allred.clj-app-simulator.ui.views.components.toast-test
    (:require [cljs.test :refer-macros [deftest testing is]]
              [com.ben-allred.clj-app-simulator.ui.views.components.toast :as toast]
              [test.utils.dom :as test.dom]
              [test.utils.spies :as spies]
              [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
              [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]))

(deftest ^:unit toast-test
    (testing "(toast)"
        (let [toast (toast/toast {:key-3 {:text  "Toast 3"
                                          :level :error}
                                  :key-1 {:text  "Toast 1"
                                          :level :warn}
                                  :key-2 {:text  "Toast 2"
                                          :level :info}})]
            (testing "has sorted toast messages"
                (let [[toast-1 toast-2 toast-3 :as toast-messages] (test.dom/query-all toast :.toast-message)]
                    (is (= 3 (count toast-messages)))
                    (test.dom/contains? toast-1 "Toast 1")
                    (test.dom/contains? toast-2 "Toast 2")
                    (test.dom/contains? toast-3 "Toast 3")))
            (testing "adds class for message level"
                (is (= 1 (count (test.dom/query-all toast :.toast-message.error))))
                (is (= 1 (count (test.dom/query-all toast :.toast-message.warn))))
                (is (= 1 (count (test.dom/query-all toast :.toast-message.info)))))
            (doseq [[idx key] [[0 :key-1] [1 :key-2] [2 :key-3]]]
                (testing (str "has remove button for toast " key)
                    (let [toast-message (nth (test.dom/query-all toast :.toast-message) idx)
                          button        (test.dom/query-one toast-message :.remove-button)
                          dispatch-spy  (spies/create)
                          action-spy    (spies/create (constantly ::action))]
                        (with-redefs [store/dispatch dispatch-spy
                                      actions/remove-toast action-spy]
                            (test.dom/simulate-event button :click)
                            (is (spies/called-with? action-spy key))
                            (is (spies/called-with? dispatch-spy ::action)))))))))
