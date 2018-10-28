(ns com.ben-allred.app-simulator.ui.simulators.http.interactions-test
  (:require
    [clojure.test :as t :refer [deftest is testing]]
    [com.ben-allred.app-simulator.templates.transformations.http :as tr]
    [com.ben-allred.app-simulator.ui.simulators.http.interactions :as interactions]
    [com.ben-allred.app-simulator.ui.simulators.shared.interactions :as shared.interactions]
    [test.utils.spies :as spies]))

(deftest ^:unit update-simulator
  (testing "(update-simulator)"
    (let [shared-spy (spies/constantly ::handler)]
      (with-redefs [shared.interactions/update-simulator shared-spy]
        (testing "updates the simulator"
          (let [handler (interactions/update-simulator ::form ::id)]
            (is (spies/called-with? shared-spy ::form tr/model->source ::id))
            (is (= handler ::handler))))))))

(deftest ^:unit reset-simulator
  (testing "(reset-simulator)"
    (let [shared-spy (spies/constantly ::handler)]
      (with-redefs [shared.interactions/reset-config shared-spy]
        (testing "resets the simulator"
          (let [handler (interactions/reset-simulator ::form ::id)]
            (is (spies/called-with? shared-spy ::form tr/sim->model ::id :http))
            (is (= handler ::handler))))))))

(deftest ^:unit create-simulator
  (testing "(create-simulator)"
    (let [shared-spy (spies/constantly ::handler)]
      (with-redefs [shared.interactions/create-simulator shared-spy]
        (testing "creates the simulator"
          (let [handler (interactions/create-simulator ::form)]
            (is (spies/called-with? shared-spy ::form tr/model->source))
            (is (= handler ::handler))))))))

(defn run-tests []
  (t/run-tests))
