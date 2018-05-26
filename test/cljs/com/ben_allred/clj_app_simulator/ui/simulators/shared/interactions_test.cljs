(ns com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions-test
  (:require [cljs.test :as t :refer [deftest testing is async]]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as shared.interactions]
            [test.utils.spies :as spies]
            [cljs.core.async :as async]
            [test.utils.dom :as test.dom]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.modals :as modals]
            [com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]
            [com.ben-allred.clj-app-simulator.ui.utils.dom :as dom]))

(deftest ^:unit do-request-test
  (testing "(do-request)"
    (async done
      (async/go
        (let [chan (async/chan)
              on-success (spies/create)
              on-failure (spies/create)]
          (testing "when status is :success"
            (let [result-ch (shared.interactions/do-request chan on-success on-failure)]
              (spies/reset! on-success on-failure)
              (async/>! chan [:success ::value])
              (async/<! result-ch)

              (testing "calls on-success"
                (is (spies/called-with? on-success ::value))
                (is (spies/never-called? on-failure)))))

          (testing "when status is not :success"
            (let [result-ch (shared.interactions/do-request chan on-success on-failure)]
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
          source-spy (spies/constantly ::source)
          action-spy (spies/constantly ::action)
          dispatch-spy (spies/constantly ::dispatch)
          reset-spy (spies/create)
          request-spy (spies/create)]
      (with-redefs [forms/current-model (constantly ::model)
                    dom/prevent-default prevent-spy
                    actions/update-simulator action-spy
                    store/dispatch dispatch-spy
                    forms/reset! reset-spy
                    shared.interactions/do-request request-spy]
        (testing "when form is submittable"
          (spies/reset! prevent-spy request-spy source-spy action-spy dispatch-spy reset-spy)
          ((shared.interactions/update-simulator ::form source-spy ::id true) ::event)

          (testing "prevents default behavior"
            (is (spies/called-with? prevent-spy ::event)))

          (testing "and when making the request"
            (let [[request on-success] (first (spies/calls request-spy))]
              (testing "dispatches an action"

                (is (spies/called-with? source-spy ::model))
                (is (spies/called-with? action-spy ::id ::source))
                (is (spies/called-with? dispatch-spy ::action))
                (is (= ::dispatch request)))

              (testing "handles success"
                (spies/reset! source-spy action-spy dispatch-spy reset-spy)
                (on-success ::ignored)

                (is (spies/called-with? reset-spy ::form ::model))))))

        (testing "when form is not submittable"
          (spies/reset! prevent-spy request-spy)
          ((shared.interactions/update-simulator ::form source-spy ::id false) ::event)

          (testing "prevents default behavior"
            (is (spies/called-with? prevent-spy ::event)))

          (testing "does not make a request"
            (is (spies/never-called? request-spy))))))))

(deftest ^:unit clear-requests-test
  (testing "(clear-requests)"
    (let [action-spy (spies/constantly ::action)
          dispatch-spy (spies/constantly ::dispatch)
          request-spy (spies/create)]
      (with-redefs [actions/clear-requests action-spy
                    store/dispatch dispatch-spy
                    shared.interactions/do-request request-spy]
        (testing "when making the request"
          (spies/reset! action-spy dispatch-spy)
          ((shared.interactions/clear-requests ::id) ::event)
          (let [[request] (first (spies/calls request-spy))]
            (testing "dispatches an action"
              (is (spies/called-with? action-spy ::id))

              (is (spies/called-with? dispatch-spy ::action))
              (is (= ::dispatch request)))))))))

(deftest ^:unit delete-sim-test
  (testing "(delete-sim)"
    (let [action-spy (spies/constantly ::action)
          dispatch-spy (spies/constantly ::dispatch)
          request-spy (spies/create)
          hide-spy (spies/create)
          nav-spy (spies/create)]
      (with-redefs [actions/delete-simulator action-spy
                    store/dispatch dispatch-spy
                    shared.interactions/do-request request-spy
                    nav/navigate! nav-spy]
        (testing "when making the request"
          (spies/reset! action-spy dispatch-spy hide-spy)
          ((shared.interactions/delete-sim ::id hide-spy) ::event)
          (let [[request on-success] (first (spies/calls request-spy))]
            (testing "deletes the simulator"
              (is (spies/called-with? action-spy ::id))
              (is (spies/called-with? dispatch-spy ::action))
              (is (= ::dispatch request)))

            (testing "handles success"
              (spies/reset! hide-spy nav-spy)
              (on-success ::ignored)
              (is (spies/called? hide-spy))
              (is (spies/called-with? nav-spy :home)))))))))

(deftest ^:unit reset-simulator-test
  (testing "(reset-simulator)"
    (let [action-spy (spies/constantly ::action)
          dispatch-spy (spies/constantly ::dispatch)
          reset-spy (spies/create)
          model-spy (spies/constantly ::model)
          request-spy (spies/create)]
      (with-redefs [actions/reset-simulator action-spy
                    store/dispatch dispatch-spy
                    forms/reset! reset-spy
                    shared.interactions/do-request request-spy]
        (testing "when making the request"
          (spies/reset! action-spy dispatch-spy reset-spy model-spy)
          ((shared.interactions/reset-simulator ::form model-spy ::id) ::event)
          (let [[request on-success] (first (spies/calls request-spy))]
            (testing "dispatches an action"

              (is (spies/called-with? action-spy ::id))
              (is (spies/called-with? dispatch-spy ::action))
              (is (= ::dispatch request)))

            (testing "handles success"
              (spies/reset! action-spy dispatch-spy reset-spy model-spy)
              (on-success ::response)

              (is (spies/called-with? model-spy ::response))
              (is (spies/called-with? reset-spy ::form ::model)))))))))

(deftest ^:unit create-simulator-test
  (testing "(create-simulator)"
    (let [prevent-spy (spies/create)
          source-spy (spies/constantly ::source)
          action-spy (spies/constantly ::action)
          dispatch-spy (spies/constantly ::dispatch)
          nav-spy (spies/create)
          request-spy (spies/create)]
      (with-redefs [forms/current-model (constantly ::model)
                    dom/prevent-default prevent-spy
                    actions/create-simulator action-spy
                    store/dispatch dispatch-spy
                    nav/nav-and-replace! nav-spy
                    shared.interactions/do-request request-spy]
        (testing "when form is submittable"
          (spies/reset! prevent-spy request-spy source-spy action-spy dispatch-spy nav-spy)
          ((shared.interactions/create-simulator ::form source-spy true) ::event)

          (testing "prevents default behavior"
            (is (spies/called-with? prevent-spy ::event)))

          (testing "and when making the request"
            (let [[request on-success] (first (spies/calls request-spy))]
              (testing "dispatches an action"

                (is (spies/called-with? source-spy ::model))
                (is (spies/called-with? action-spy ::source))
                (is (spies/called-with? dispatch-spy ::action))
                (is (= ::dispatch request)))

              (testing "handles success"
                (spies/reset! source-spy action-spy dispatch-spy nav-spy)
                (on-success {:simulator {:id ::id}})

                (is (spies/called-with? nav-spy :details {:id ::id}))))))

        (testing "when form is not submittable"
          (spies/reset! prevent-spy request-spy)
          ((shared.interactions/create-simulator ::form source-spy false) ::event)

          (testing "prevents default behavior"
            (is (spies/called-with? prevent-spy ::event)))

          (testing "does not make a request"
            (is (spies/never-called? request-spy))))))))

(deftest ^:unit show-delete-modal-test
  (testing "(show-delete-modal)"
    (let [action-spy (spies/constantly ::action)
          delete-spy (spies/constantly ::delete)
          dispatch-spy (spies/create)]
      (with-redefs [actions/show-modal action-spy
                    shared.interactions/delete-sim delete-spy
                    store/dispatch dispatch-spy]
        (testing "when taking the action"
          ((shared.interactions/show-delete-modal ::id) ::event)
          (testing "shows a modal"
            (is (spies/called-with? action-spy
                                    [modals/confirm-delete]
                                    "Delete Simulator"
                                    (spies/matcher vector?)
                                    (spies/matcher vector?)))
            (is (spies/called-with? dispatch-spy ::action)))

          (let [tree (->> action-spy
                          (spies/calls)
                          (first)
                          (filter vector?)
                          (into [:div]))
                close-btn (test.dom/query-one tree :.cancel-button)
                delete-btn (test.dom/query-one tree :.delete-button)]
            (testing "gives the modal a close button"
              (is (test.dom/query-one close-btn :.button.button-secondary.pure-button))
              (is (test.dom/contains? close-btn "Cancel")))

            (testing "gives the modal a button which deletes the simulator"
              (let [f (:on-click (test.dom/attrs delete-btn))]
                (f ::hide)
                (is (spies/called-with? delete-spy ::id ::hide))))))))))

(defn run-tests [] (t/run-tests))
