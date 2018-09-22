(ns com.ben-allred.clj-app-simulator.ui.services.forms.core-test
  (:require [clojure.test :as t :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]
            [test.utils.spies :as spies]))

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
            (is (forms/changed? form)))

          (testing "and when supplying an empty path"
            (forms/assoc-in form [] {:new :map})
            (testing "has an initial model"
              (is (= {:some :data} (forms/initial-model form))))

            (testing "has a current model"
              (is (= {:new :map} (forms/current-model form))))

            (testing "has changes"
              (is (forms/changed? form)))))

        (testing "and when using update-in"
          (forms/update-in form [:update-in :path] assoc :a 1)
          (testing "has an initial model"
            (is (= {:some :data} (forms/initial-model form))))

          (testing "has a current model"
            (is (= {:new :map :update-in {:path {:a 1}}} (forms/current-model form))))

          (testing "has changes"
            (is (forms/changed? form)))

          (testing "and when supplying an empty path"
            (forms/update-in form [] dissoc :new)
            (testing "has an initial model"
              (is (= {:some :data} (forms/initial-model form))))

            (testing "has a current model"
              (is (= {:update-in {:path {:a 1}}} (forms/current-model form))))

            (testing "has changes"
              (is (forms/changed? form)))))

        (testing "and when resetting the form"
          (forms/reset! form {:re :set})
          (testing "has an initial model"
            (is (= {:re :set} (forms/initial-model form))))

          (testing "has a current model"
            (is (= {:re :set} (forms/current-model form))))

          (testing "has not changed"
            (is (not (forms/changed? form))))))

      (testing "and when including a validator"
        (let [validator (spies/constantly ::errors)
              form (forms/create {:a :model} validator)]
          (testing "calls the validator with the model"
            (is (= ::errors (forms/errors form)))
            (is (spies/called-with? validator {:a :model})))

          (testing "and when the model changes"
            (spies/reset! validator)
            (spies/respond-with! validator (constantly ::new-errors))
            (forms/assoc-in form [:with] :data)
            (testing "updates the errors"
              (is (= ::new-errors (forms/errors form))))

            (testing "calls the validator with the new model"
              (is (spies/called-with? validator {:a :model :with :data})))))))))

(defn run-tests [] (t/run-tests))
