(ns com.ben-allred.clj-app-simulator.ui.simulators.ws.interactions-test
  (:require [cljs.test :as t :refer [deftest testing is]]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as shared.interactions]
            [com.ben-allred.clj-app-simulator.ui.simulators.ws.interactions :as interactions]
            [com.ben-allred.clj-app-simulator.ui.simulators.ws.transformations :as tr]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]))

(deftest ^:unit update-simulator-test
  (testing "(update-simulator)"
    (let [update-spy (spies/create (constantly ::update))]
      (with-redefs [shared.interactions/update-simulator update-spy]
        (testing "updates the simulator"
          (let [handler (interactions/update-simulator ::form ::id ::submittable?)]
            (is (spies/called-with? update-spy ::form tr/model->source ::id ::submittable?))
            (is (= ::update handler))))))))

(deftest ^:unit reset-simulator-test
  (testing "(reset-simulator)"
    (let [reset-spy (spies/create (constantly ::reset))]
      (with-redefs [shared.interactions/reset-simulator reset-spy]
        (testing "resets the simulator"
          (let [handler (interactions/reset-simulator ::form ::id)]
            (is (spies/called-with? reset-spy ::form tr/sim->model ::id))
            (is (= ::reset handler))))))))

(deftest ^:unit create-simulator-test
  (testing "(create-simulator)"
    (let [create-spy (spies/create (constantly ::create))]
      (with-redefs [shared.interactions/create-simulator create-spy]
        (testing "creates the simulator"
          (let [handler (interactions/create-simulator ::form ::submittable?)]
            (is (spies/called-with? create-spy ::form tr/model->source ::submittable?))
            (is (= ::create handler))))))))

(deftest ^:unit disconnect-all-test
  (testing "(disconnect-all)"
    (let [action-spy (spies/create (constantly ::action))
          dispatch-spy (spies/create (constantly ::dispatch))
          request-spy (spies/create)]
      (with-redefs [actions/disconnect-all action-spy
                    store/dispatch dispatch-spy
                    shared.interactions/do-request request-spy]
        (testing "disconnects all sockets"
          ((interactions/disconnect-all ::id) ::event)
          (is (spies/called-with? action-spy ::id))
          (is (spies/called-with? dispatch-spy ::action))
          (is (spies/called-with? request-spy ::dispatch)))))))

(defn run-tests [] (t/run-tests))
