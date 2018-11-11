(ns com.ben-allred.app-simulator.ui.services.forms.core-test
  (:require
    [clojure.test :as t :refer [deftest is testing]]
    [com.ben-allred.app-simulator.ui.services.forms.core :as forms]
    [test.utils.spies :as spies]
    [test.utils.dom :as test.dom]
    [com.ben-allred.app-simulator.templates.views.core :as views]))

(deftest ^:unit sync-button-test
  (testing "(sync-button)"
    (with-redefs [gensym (constantly ::id)
                  forms/syncing? (spies/create)
                  forms/errors (spies/create)
                  forms/sync! (spies/create)]
      (let [on-click (spies/create)]
        (testing "when the form is syncing"
          (spies/respond-with! forms/syncing? (constantly true))
          (let [[_ attrs content :as button] (-> {:form      ::form
                                                  :text      ::text
                                                  :sync-text ::sync-text
                                                  ::other    ::attrs
                                                  :on-click  on-click}
                                                 (forms/sync-button)
                                                 (test.dom/query-one :.button.sync-button))]
            (testing "is disabled"
              (is (:disabled attrs)))

            (testing "displays the sync-text"
              (let [content (test.dom/query-one content :.syncing)]
                (is (test.dom/contains? content ::sync-text))
                (is (test.dom/contains? content [views/spinner]))))

            (testing "and when the button is clicked"
              (test.dom/simulate-event button :click)
              (testing "syncs the form"
                (is (spies/called-with? forms/errors ::form))
                (is (spies/called-with? forms/sync! ::form))))))

        (testing "when the form is not syncing"
          (spies/respond-with! forms/syncing? (constantly false))
          (let [[_ attrs content] (-> {:form      ::form
                                       :text      ::text
                                       :sync-text ::sync-text
                                       ::other    ::attrs
                                       :on-click  on-click}
                                      (forms/sync-button)
                                      (test.dom/query-one :.button.sync-button))]
            (testing "is not disabled"
              (is (not (:disabled attrs))))

            (testing "displays the text"
              (is (= content ::text)))))))))

(defn run-tests []
  (t/run-tests))
