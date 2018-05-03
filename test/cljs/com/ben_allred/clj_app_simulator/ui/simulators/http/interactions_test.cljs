(ns com.ben-allred.clj-app-simulator.ui.simulators.http.interactions-test
  (:require [cljs.test :as t :refer [deftest testing is async]]
            [cljs.core.async :as async]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.interactions :as interactions]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.transformations :as tr]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.utils.dom :as dom]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.modals :as modals]
            [test.utils.dom :as test.dom]
            [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]))

(deftest ^:unit do-request-test
  (testing "(do-request)"
    (async done
      (async/go
        (let [chan (async/chan)
              on-success (spies/create)
              on-failure (spies/create)]
          (testing "when status is :success"
            (let [result-ch (interactions/do-request (constantly chan) on-success on-failure)]
              (spies/reset! on-success on-failure)
              (async/>! chan [:success ::value])
              (async/<! result-ch)

              (testing "calls on-success"
                (is (spies/called-with? on-success ::value))
                (is (spies/never-called? on-failure)))))

          (testing "when status is not :success"
            (let [result-ch (interactions/do-request (constantly chan) on-success on-failure)]
              (spies/reset! on-success on-failure)
              (async/>! chan [:error ::value])
              (async/<! result-ch)

              (testing "calls on-failure"
                (is (spies/called-with? on-failure ::value))
                (is (spies/never-called? on-success)))))

          (done))))))

(deftest ^:unit update-simulator-test
  (testing "(update-simulator)"
    (let [prevent-spy (spies/create)
          source-spy (spies/create (constantly ::source))
          action-spy (spies/create (constantly ::action))
          dispatch-spy (spies/create)
          reset-spy (spies/create)
          request-spy (spies/create)]
      (with-redefs [forms/current-model (constantly ::model)
                    dom/prevent-default prevent-spy
                    tr/model->source source-spy
                    actions/update-simulator action-spy
                    store/dispatch dispatch-spy
                    forms/reset! reset-spy
                    interactions/do-request request-spy]
        (testing "when form is submittable"
          (spies/reset! prevent-spy request-spy)
          ((interactions/update-simulator ::form ::id true) ::event)

          (testing "prevents default behavior"
            (is (spies/called-with? prevent-spy ::event)))

          (testing "and when making the request"
            (let [[request on-success] (first (spies/calls request-spy))]
              (testing "dispatches an action"
                (spies/reset! source-spy action-spy dispatch-spy reset-spy)
                (request)

                (is (spies/called-with? source-spy ::model))
                (is (spies/called-with? action-spy ::id ::source))
                (is (spies/called-with? dispatch-spy ::action)))

              (testing "handles success"
                (spies/reset! source-spy action-spy dispatch-spy reset-spy)
                (on-success ::ignored)

                (is (spies/called-with? reset-spy ::form ::model))))))

        (testing "when form is not submittable"
          (spies/reset! prevent-spy request-spy)
          ((interactions/update-simulator ::form ::id false) ::event)

          (testing "prevents default behavior"
            (is (spies/called-with? prevent-spy ::event)))

          (testing "does not make a request"
            (is (spies/never-called? request-spy))))))))

(deftest ^:unit reset-simulator-test
  (testing "(reset-simulator)"
    (let [action-spy (spies/create (constantly ::action))
          dispatch-spy (spies/create)
          reset-spy (spies/create)
          model-spy (spies/create (constantly ::model))
          request-spy (spies/create)]
      (with-redefs [actions/reset-simulator action-spy
                    store/dispatch dispatch-spy
                    forms/reset! reset-spy
                    tr/sim->model model-spy
                    interactions/do-request request-spy]
        (testing "when making the request"
          ((interactions/reset-simulator ::form ::id) ::event)
          (let [[request on-success] (first (spies/calls request-spy))]
            (testing "dispatches an action"
              (spies/reset! action-spy dispatch-spy reset-spy model-spy)
              (request)

              (is (spies/called-with? action-spy ::id))
              (is (spies/called-with? dispatch-spy ::action)))

            (testing "handles success"
              (spies/reset! action-spy dispatch-spy reset-spy model-spy)
              (on-success ::response)

              (is (spies/called-with? model-spy ::response))
              (is (spies/called-with? reset-spy ::form ::model)))))))))

(deftest ^:unit clear-requests-test
  (testing "(clear-requests)"
    (let [action-spy (spies/create (constantly ::action))
          dispatch-spy (spies/create)
          request-spy (spies/create)]
      (with-redefs [actions/clear-requests action-spy
                    store/dispatch dispatch-spy
                    interactions/do-request request-spy]
        (testing "when making the request"
          ((interactions/clear-requests ::id) ::event)
          (let [[request] (first (spies/calls request-spy))]
            (testing "dispatches an action"
              (spies/reset! action-spy dispatch-spy)
              (request)

              (is (spies/called-with? action-spy ::id))
              (is (spies/called-with? dispatch-spy ::action)))))))))

(deftest ^:unit delete-sim-test
  (testing "(delete-sim)"
    (let [action-spy (spies/create (constantly ::action))
          dispatch-spy (spies/create)
          request-spy (spies/create)
          hide-spy (spies/create)
          nav-spy (spies/create)]
      (with-redefs [actions/delete-simulator action-spy
                    store/dispatch dispatch-spy
                    interactions/do-request request-spy
                    nav/navigate! nav-spy]
        (testing "when making the request"
          ((interactions/delete-sim ::id hide-spy) ::event)
          (let [[request on-success] (first (spies/calls request-spy))]
            (testing "deletes the simulator"
              (spies/reset! action-spy dispatch-spy hide-spy)
              (request)
              (is (spies/called-with? action-spy ::id))
              (is (spies/called-with? dispatch-spy ::action)))

            (testing "handles success"
              (spies/reset! action-spy dispatch-spy hide-spy nav-spy)
              (on-success ::ignored)
              (is (spies/called? hide-spy))
              (is (spies/called-with? nav-spy :home)))))))))

(deftest ^:unit show-delete-modal-test
  (testing "(show-delete-modal)"
    (let [action-spy (spies/create (constantly ::action))
          delete-spy (spies/create (constantly ::delete))
          dispatch-spy (spies/create)]
      (with-redefs [actions/show-modal action-spy
                    interactions/delete-sim delete-spy
                    store/dispatch dispatch-spy]
        (testing "when taking the action"
          ((interactions/show-delete-modal ::id) ::event)
          (testing "shows a modal"
            (is (spies/called-with? action-spy
                                    [modals/confirm-delete]
                                    "Delete Simulator"
                                    (spies/matcher vector?)
                                    (spies/matcher vector?)))
            (is (spies/called-with? dispatch-spy ::action)))

          (let [[_ _ close-btn delete-btn] (first (spies/calls action-spy))]
            (testing "gives the modal a close button"
              (is (test.dom/query-one close-btn :.button.button-secondary.pure-button))
              (is (test.dom/contains? close-btn "Cancel")))

            (testing "gives the modal a button which deletes the simulator"
              (let [f (:on-click (test.dom/attrs delete-btn))]
                (f ::hide)
                (is (spies/called-with? delete-spy ::id ::hide))))))))))

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
