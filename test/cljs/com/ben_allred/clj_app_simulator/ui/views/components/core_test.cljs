(ns com.ben-allred.clj-app-simulator.ui.views.components.core-test
    (:require [cljs.test :refer-macros [deftest testing is]]
              [test.utils.dom :as test.dom]
              [com.ben-allred.clj-app-simulator.ui.views.components.core :as components]))

(deftest spinner-overlay-test
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
