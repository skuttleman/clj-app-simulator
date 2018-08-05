(ns com.ben-allred.clj-app-simulator.ui.simulators.file.interactions-test
  (:require [cljs.test :as t :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.ui.simulators.file.interactions :as interactions]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.ui.simulators.file.transformations :as tr]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as shared.interactions]))

(deftest ^:unit update-simulator
  (testing "(update-simulator)"
    (let [shared-spy (spies/constantly ::handler)]
      (with-redefs [shared.interactions/update-simulator shared-spy]
        (testing "updates the simulator"
          (let [handler (interactions/update-simulator ::form ::id ::submittable?)]
            (is (spies/called-with? shared-spy ::form tr/model->source ::id ::submittable?))
            (is (= handler ::handler))))))))

(deftest ^:unit reset-simulator
  (testing "(reset-simulator)"
    (let [shared-spy (spies/constantly ::handler)]
      (with-redefs [shared.interactions/reset-simulator shared-spy]
        (testing "resets the simulator"
          (let [handler (interactions/reset-simulator ::form ::id)]
            (is (spies/called-with? shared-spy ::form tr/sim->model ::id))
            (is (= handler ::handler))))))))

(deftest ^:unit create-simulator
  (testing "(create-simulator)"
    (let [shared-spy (spies/constantly ::handler)]
      (with-redefs [shared.interactions/create-simulator shared-spy]
        (testing "creates the simulator"
          (let [handler (interactions/create-simulator ::form ::submittable?)]
            (is (spies/called-with? shared-spy ::form tr/model->source ::submittable?))
            (is (= handler ::handler))))))))

(defn run-tests []
  (t/run-tests))
