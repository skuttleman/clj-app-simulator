(ns com.ben-allred.app-simulator.ui.services.forms.standard-test
  (:require
    [clojure.test :as t :refer [deftest is testing]]
    [com.ben-allred.app-simulator.ui.services.forms.core :as forms]
    [com.ben-allred.app-simulator.ui.services.forms.standard :as form]
    [test.utils.spies :as spies]))

(deftest ^:unit create-test
  (testing "(create)"
    (testing "satisfies ISync"
      (let [form (form/create ::model)]
        (forms/sync! form)
        (is (forms/syncing? form))
        (forms/ready! form)
        (is (not (forms/syncing? form))))

      (testing "satisfies IChange"
        (let [form (form/create {:some {:path 0}})]
          (is (not (forms/touched? form)))

          (forms/assoc-in! form [:some :path] 0)
          (is (forms/touched? form))
          (is (forms/touched? form [:some :path]))
          (is (not (forms/touched? form [:another :path])))
          (is (not (forms/changed? form)))

          (forms/update-in! form [:some :path] + 1)
          (is (forms/changed? form))
          (is (forms/changed? form [:some :path]))
          (is (not (forms/changed? form [:another :path])))))

      (testing "satisfies IVerify"
        (let [form (form/create ::model)]
          (is (not (forms/verified? form)))
          (forms/verify! form)
          (is (forms/verified? form))))

      (testing "satisfies IValidate"
        (is (nil? (forms/errors (form/create ::model))))
        (let [validator (spies/constantly ::errors)
              form (form/create ::model validator)
              errors (forms/errors form)]
          (is (= ::errors errors))
          (is (spies/called-with? validator ::model))))

      (testing "satisfies IReset"
        (let [form (form/create {:a :model})]
          (forms/sync! form)
          (forms/assoc-in! form [:a] :new-model)
          (forms/verify! form)
          (reset! form {:a :reset-model})
          (is (not (forms/syncing? form)))
          (is (not (forms/verified? form)))
          (is (not (forms/touched? form)))
          (is (not (forms/changed? form)))))

      (testing "satisfies IDeref"
        (let [form (form/create 2)]
          (is (= 2 @form))
          (forms/update-in! form [] - 3)
          (is (= -1 @form)))))))

(defn run-tests []
  (t/run-tests))
