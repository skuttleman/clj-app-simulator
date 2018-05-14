(ns com.ben-allred.clj-app-simulator.ui.simulators.http.interactions-test
  (:require [cljs.test :as t :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.interactions :as interactions]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.transformations :as tr]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.modals :as modals]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as shared.interactions]))

(deftest ^:unit update-simulator
  (testing "(update-simulator)"
    (let [shared-spy (spies/create (constantly ::handler))]
      (with-redefs [shared.interactions/update-simulator shared-spy]
        (testing "updates the simulator"
          (let [handler (interactions/update-simulator ::form ::id ::submittable?)]
            (is (spies/called-with? shared-spy ::form tr/model->source ::id ::submittable?))
            (is (= handler ::handler))))))))

(deftest ^:unit reset-simulator
  (testing "(reset-simulator)"
    (let [shared-spy (spies/create (constantly ::handler))]
      (with-redefs [shared.interactions/reset-simulator shared-spy]
        (testing "resets the simulator"
          (let [handler (interactions/reset-simulator ::form ::id)]
            (is (spies/called-with? shared-spy ::form tr/model->source ::id))
            (is (= handler ::handler))))))))

(deftest ^:unit create-simulator
  (testing "(create-simulator)"
    (let [shared-spy (spies/create (constantly ::handler))]
      (with-redefs [shared.interactions/create-simulator shared-spy]
        (testing "creates the simulator"
          (let [handler (interactions/create-simulator ::form ::submittable?)]
            (is (spies/called-with? shared-spy ::form tr/model->source ::submittable?))
            (is (= handler ::handler))))))))

(deftest ^:unit show-request-modal-test
  (testing "(show-request-modal)"
    (let [action-spy (spies/create (constantly ::action))
          dispatch-spy (spies/create)]
      (with-redefs [actions/show-modal action-spy
                    store/dispatch dispatch-spy]
        (testing "when making the request"
          ((interactions/show-request-modal ::sim {::some ::request} ::dt) ::event)
          (testing "dispatches an action"
            (is (spies/called-with? action-spy
                                    [modals/request-modal ::sim {::some ::request :dt ::dt}]
                                    "Request Details"))
            (is (spies/called-with? dispatch-spy ::action))))))))

(defn run-tests [] (t/run-tests))
