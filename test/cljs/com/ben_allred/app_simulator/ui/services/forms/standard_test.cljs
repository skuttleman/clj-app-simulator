(ns com.ben-allred.app-simulator.ui.services.forms.standard-test
  (:require
    [clojure.test :as t :refer [deftest is testing]]
    [com.ben-allred.app-simulator.services.forms.core :as forms]
    [com.ben-allred.app-simulator.ui.services.forms.standard :as form.std]
    [test.utils.spies :as spies]))

(deftest ^:unit create-test
  (testing "(create)"
    (testing "satisfies ISync"
      (let [form (form.std/create ::model)]
        (forms/sync! form)
        (is (forms/syncing? form))
        (forms/ready! form ::value)
        (is (not (forms/syncing? form)))))

    (testing "satisfies IChange"
      (let [form (form.std/create {:some {:path 0}})]
        (is (not (forms/touched? form)))

        (swap! form assoc-in [:some :path] 1)
        (is (forms/touched? form))
        (is (forms/touched? form [:some :path]))
        (is (not (forms/touched? form [:another :path])))
        (is (forms/changed? form))))

    (testing "satisfies IValidate"
      (is (nil? (forms/errors (form.std/create ::model))))
      (let [validator (spies/constantly ::errors)
            form (form.std/create ::model validator)
            errors (forms/errors form)]
        (is (= ::errors errors))
        (is (spies/called-with? validator ::model))))

    (testing "satisfies IReset"
      (let [form (form.std/create {:a :model})]
        (forms/sync! form)
        (swap! form assoc-in [:a] :new-model)
        (reset! form {:a :reset-model})
        (is (not (forms/syncing? form)))
        (is (not (forms/touched? form)))
        (is (not (forms/changed? form)))))

    (testing "satisfies IDeref"
      (let [form (form.std/create {:a 2})]
        (is (= {:a 2} @form))
        (swap! form update-in [:a] - 3)
        (is (= {:a -1} @form))))))

(defn run-tests []
  (t/run-tests))
