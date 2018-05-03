(ns com.ben-allred.clj-app-simulator.ui.views.components.core-test
  (:require [cljs.test :as t :refer-macros [deftest testing is]]
            [test.utils.dom :as test.dom]
            [com.ben-allred.clj-app-simulator.ui.views.components.core :as components]
            [test.utils.spies :as spies]))

(deftest ^:unit spinner-overlay-test
  (testing "(spinner-overlay)"
    (testing "when shown"
      (let [spinner (components/spinner-overlay true ::component)]
        (testing "has spinner component"
          (let [node (test.dom/query-one spinner :.spinner-container)]
            (test.dom/contains? node components/spinner)))
        (testing "has passed component"
          (let [node (test.dom/query-one spinner :.component-container)]
            (test.dom/contains? node ::component)))))
    (testing "when not show"
      (let [spinner (components/spinner-overlay false ::component)]
        (testing "does not have spinner component"
          (is (not (test.dom/contains? spinner components/spinner))))
        (testing "does not have passed component"
          (is (not (test.dom/contains? spinner ::component))))))))

(deftest ^:unit with-status-test
  (testing "(with-status)"
    (testing "when item is empty"
      (let [request-spy (spies/create)]
        (components/with-status ::status ::component {} request-spy)
        (testing "invokes request"
          (is (spies/called? request-spy)))))
    (testing "when item is not empty"
      (let [request-spy (spies/create)]
        (components/with-status ::status ::component {::some ::item} request-spy)
        (testing "does not invoke request"
          (spies/never-called? request-spy))))
    (testing "when status is :available and there is an item"
      (let [args [:available ::component {::some ::item} ::request]
            root (apply (apply components/with-status args) args)]
        (testing "renders the component"
          (is (= [::component {::some ::item}]
                 (test.dom/query-one root ::component))))))
    (testing "when there is no item and the status is available"
      (let [args [:available ::component nil (spies/create)]
            root (apply (apply components/with-status args) args)]
        (testing "renders an error message"
          (is (test.dom/contains? root "Not found")))))
    (testing "when the status is any other value"
      (let [args [::random-status ::component nil (spies/create)]
            root (apply (apply components/with-status args) args)]
        (testing "renders a spinner"
          (is (test.dom/query-one root components/spinner)))))))

(defn run-tests [] (t/run-tests))
