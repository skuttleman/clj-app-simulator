(ns com.ben-allred.clj-app-simulator.ui.services.forms.core-test
  (:require
    [clojure.test :as t :refer [deftest is testing]]
    [com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]
    [test.utils.spies :as spies]
    [test.utils.dom :as test.dom]
    [com.ben-allred.clj-app-simulator.templates.views.core :as views]))

(deftest create-test
  (testing "(create)"
    (testing "when creating an IForm"
      (let [form (forms/create {:some :data})]
        (testing "has an initial model"
          (is (= {:some :data} (forms/initial-model form))))

        (testing "has a current model"
          (is (= {:some :data} (forms/current-model form))))

        (testing "has not changed"
          (is (not (forms/changed? form))))

        (testing "and when using assoc-in"
          (forms/assoc-in form [:numbers :one] 1)
          (testing "has an initial model"
            (is (= {:some :data} (forms/initial-model form))))

          (testing "has a current model"
            (is (= {:some :data :numbers {:one 1}} (forms/current-model form))))

          (testing "has changes"
            (is (forms/changed? form))))

        (testing "and when using update-in"
          (forms/update-in form [:update-in :path] assoc :a 1)
          (testing "has an initial model"
            (is (= {:some :data} (forms/initial-model form))))

          (testing "has a current model"
            (is (= {:some :data :numbers {:one 1} :update-in {:path {:a 1}}} (forms/current-model form))))

          (testing "has changes"
            (is (forms/changed? form))))

        (testing "and when resetting the form"
          (forms/reset! form {:re :set})
          (testing "has an initial model"
            (is (= {:re :set} (forms/initial-model form))))

          (testing "has a current model"
            (is (= {:re :set} (forms/current-model form))))

          (testing "has not changed"
            (is (not (forms/changed? form))))))

      (testing "and when including a validator"
        (let [validator (spies/constantly {:a ::errors})
              form (forms/create {:a :model} validator)]
          (testing "calls the validator with the model"
            (is (= nil (forms/display-errors form)))
            (is (= {:a ::errors} (forms/errors form)))
            (is (spies/called-with? validator {:a :model})))

          (testing "and when the model changes"
            (spies/reset! validator)
            (spies/respond-with! validator (constantly {:a ::errors :with ::new-errors}))
            (forms/assoc-in form [:with] :data)
            (testing "updates the errors"
              (is (= {:with ::new-errors} (forms/display-errors form)))
              (is (= {:a ::errors :with ::new-errors} (forms/errors form))))

            (testing "calls the validator with the new model"
              (is (spies/called-with? validator {:a :model :with :data})))))))))

(deftest ^:unit sync-button-test
  (testing "(sync-button)"
    (let [syncing-spy (spies/create)
          errors-spy (spies/create)
          sync-spy (spies/create)
          on-click (spies/create)]
      (with-redefs [gensym (constantly ::id)
                    forms/syncing? syncing-spy
                    forms/errors errors-spy
                    forms/sync! sync-spy]
        (let [sync-button (forms/sync-button nil)]
          (testing "when the form is syncing"
            (spies/respond-with! syncing-spy (constantly ::syncing))
            (let [[_ attrs content :as button] (-> (sync-button {:form      ::form
                                                                 :text      ::text
                                                                 :sync-text ::sync-text
                                                                 ::other    ::attrs
                                                                 :on-click  on-click})
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
                  (is (spies/called-with? errors-spy ::form))
                  (is (spies/called-with? sync-spy ::form ::id))))))

          (testing "when the form is not syncing"
            (spies/respond-with! syncing-spy (constantly nil))
            (let [[_ attrs content] (-> (sync-button {:form      ::form
                                                      :text      ::text
                                                      :sync-text ::sync-text
                                                      ::other    ::attrs
                                                      :on-click  on-click})
                                        (test.dom/query-one :.button.sync-button))]
              (testing "is not disabled"
                (is (not (:disabled attrs))))

              (testing "displays the text"
                (is (= content ::text))))))))))

(defn run-tests []
  (t/run-tests))
