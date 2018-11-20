(ns com.ben-allred.app-simulator.ui.simulators.http.interactions-test
  (:require
    [clojure.test :as t :refer [deftest is testing]]
    [com.ben-allred.app-simulator.templates.transformations.http :as tr]
    [com.ben-allred.app-simulator.ui.simulators.http.interactions :as interactions]
    [com.ben-allred.app-simulator.ui.simulators.shared.interactions :as shared.interactions]
    [test.utils.spies :as spies]))

(deftest ^:unit update-simulator
  (testing "(update-simulator)"
    (with-redefs [shared.interactions/update-simulator (spies/constantly ::handler)]
      (testing "updates the simulator"
        (let [handler (interactions/update-simulator ::form ::id)]
          (is (spies/called-with? shared.interactions/update-simulator ::form tr/model->source tr/source->model ::id))
          (is (= handler ::handler)))))))

(deftest ^:unit reset-simulator
  (testing "(reset-simulator)"
    (with-redefs [shared.interactions/reset-config (spies/constantly ::handler)]
      (testing "resets the simulator"
        (let [handler (interactions/reset-simulator ::id)]
          (is (spies/called-with? shared.interactions/reset-config tr/source->model ::id :http))
          (is (= handler ::handler)))))))

(deftest ^:unit create-simulator
  (testing "(create-simulator)"
    (with-redefs [shared.interactions/create-simulator (spies/constantly ::handler)]
      (testing "creates the simulator"
        (let [handler (interactions/create-simulator ::form)]
          (is (spies/called-with? shared.interactions/create-simulator ::form tr/model->source))
          (is (= handler ::handler)))))))

(defn run-tests []
  (t/run-tests))
