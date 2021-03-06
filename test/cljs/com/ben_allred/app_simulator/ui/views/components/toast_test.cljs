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
    (let [toast (-> {:key-3 {:ref   (atom "Toast 3")
                             :level :success}
                     :key-5 {:ref   (atom "Toast 5")
                             :level :error}
                     :key-1 {:ref   (atom "Toast 1")
                             :level :success}
                     :key-2 {:ref   (atom "Toast 2")
                             :level :error}
                     :key-4 {:ref   (atom "Toast 4")
                             :level :success}}
                    (toast/toast)
                    (update-in [1 1] (partial map (fn [[f & args]]
                                                    (apply (apply f args) args)))))]
      (let [[toast-1 toast-2 toast-3 :as toast-messages] (test.dom/query-all toast :.toast-message)]
        (testing "limits display of toasts"
          (is (= 3 (count toast-messages)))
          (is (not (test.dom/contains? toast-messages "Toast 5"))))

        (testing "has sorted toast messages"
          (is (test.dom/contains? toast-1 "Toast 1"))
          (is (test.dom/contains? toast-2 "Toast 2"))
          (is (test.dom/contains? toast-3 "Toast 3"))))

      (testing "adds class for message level"
        (is (= 1 (count (test.dom/query-all toast :.toast-message.is-danger))))
        (is (= 2 (count (test.dom/query-all toast :.toast-message.is-success)))))

      (doseq [[idx key] (map-indexed vector [:key-1 :key-2 :key-3])]
        (testing (str "has remove button for toast " key)
          (let [toast-message (nth (test.dom/query-all toast :.toast-message) idx)
                header (test.dom/query-one toast-message :.message-header)]
            (with-redefs [store/dispatch (spies/create)
                          actions/remove-toast (spies/constantly ::action)]
              (test.dom/simulate-event header :click)
              (is (spies/called-with? actions/remove-toast key))
              (is (spies/called-with? store/dispatch ::action)))))))))

(defn run-tests []
  (t/run-tests))
