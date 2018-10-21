(ns com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions-test
  (:require
    [cljs.core.async :as async]
    [clojure.test :as t :refer [async deftest is testing]]
    [com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]
    [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
    [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
    [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
    [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as shared.interactions]
    [com.ben-allred.clj-app-simulator.ui.utils.dom :as dom]
    [test.utils.dom :as test.dom]
    [test.utils.spies :as spies]))

(deftest ^:unit toaster-test
  (testing "(toaster)"
    (let [show-toast-spy (spies/constantly ::action)
          dispatch-spy (spies/create)]
      (with-redefs [actions/show-toast show-toast-spy
                    store/dispatch dispatch-spy]
        (let [toast (shared.interactions/toaster ::level ::default-message)]
          (testing "returns a function that returns the body"
            (is (= ::body (toast ::body))))

          (testing "when the body has a message"
            (spies/reset! show-toast-spy dispatch-spy)
            (toast {:message ::some-message})

            (testing "toasts the message"
              (is (spies/called-with? show-toast-spy ::level ::some-message))
              (is (spies/called-with? dispatch-spy ::action))))

          (testing "when the body has no message"
            (spies/reset! show-toast-spy dispatch-spy)
            (toast {})

            (testing "toasts the default message"
              (is (spies/called-with? show-toast-spy ::level ::default-message))
              (is (spies/called-with? dispatch-spy ::action)))))))))

(deftest ^:unit resetter-test
  (testing "(resetter)"
    (let [spy (spies/create)
          reset (shared.interactions/resetter spy ::form ::arg-1 ::arg-2 ::arg-3)
          result (reset ::body)]
      (testing "applies f with args"
        (is (spies/called-with? spy ::form ::arg-1 ::arg-2 ::arg-3)))

      (testing "returns the body"
        (is (= ::body result))))))

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
          verify-spy (spies/create)
          errors-spy (spies/create)
          changed-spy (spies/constantly true)
          request-spy (spies/create)]
      (with-redefs [forms/current-model (constantly ::model)
                    dom/prevent-default prevent-spy
                    actions/update-simulator action-spy
                    store/dispatch dispatch-spy
                    forms/reset! reset-spy
                    forms/verify! verify-spy
                    forms/errors errors-spy
                    forms/changed? changed-spy
                    shared.interactions/do-request request-spy]
        (testing "when form is submittable"
          (spies/reset! prevent-spy request-spy source-spy action-spy dispatch-spy reset-spy)
          ((shared.interactions/update-simulator ::form source-spy ::id) ::event)

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

        (testing "when form has errors"
          (spies/reset! prevent-spy request-spy)
          (spies/respond-with! errors-spy (constantly ::errors))
          ((shared.interactions/update-simulator ::form source-spy ::id) ::event)

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
        (testing "when making the request with type :http"
          (spies/reset! action-spy dispatch-spy)
          ((shared.interactions/clear-requests :http ::id) ::event)
          (let [[request] (first (spies/calls request-spy))]
            (testing "dispatches an action"
              (is (spies/called-with? action-spy :simulators.http/reset-requests ::id))
              (is (spies/called-with? dispatch-spy ::action))
              (is (= ::dispatch request)))))

        (testing "when making the request with type :ws"
          (spies/reset! action-spy dispatch-spy)
          ((shared.interactions/clear-requests :ws ::id) ::event)
          (let [[request] (first (spies/calls request-spy))]
            (testing "dispatches an action"
              (is (spies/called-with? action-spy :simulators.ws/reset-messages ::id))
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
          (((shared.interactions/delete-sim ::id) hide-spy) ::event)
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
    (let [error-spy (spies/constantly nil)
          prevent-spy (spies/create)
          source-spy (spies/constantly ::source)
          action-spy (spies/constantly ::action)
          dispatch-spy (spies/constantly ::dispatch)
          nav-spy (spies/create)
          request-spy (spies/create)
          resetter-spy (spies/create (constantly identity))]
      (with-redefs [forms/current-model (constantly ::model)
                    forms/errors error-spy
                    forms/verify! (constantly nil)
                    dom/prevent-default prevent-spy
                    actions/create-simulator action-spy
                    store/dispatch dispatch-spy
                    nav/nav-and-replace! nav-spy
                    shared.interactions/do-request request-spy
                    shared.interactions/resetter resetter-spy]
        (testing "when form is submittable"
          (spies/reset! prevent-spy request-spy source-spy action-spy dispatch-spy nav-spy)
          ((shared.interactions/create-simulator ::form source-spy) ::event)

          (testing "prevents default behavior"
            (is (spies/called-with? prevent-spy ::event)))

          (testing "and when making the request"
            (let [[request on-success] (first (spies/calls request-spy))]
              (testing "dispatches an action"

                (is (spies/called-with? source-spy ::model))
                (is (spies/called-with? action-spy ::source))
                (is (spies/called-with? dispatch-spy ::action))
                (is (spies/called-with? resetter-spy forms/reset! ::form ::model))
                (is (spies/called-with? resetter-spy forms/ready! ::form))
                (is (= ::dispatch request)))

              (testing "handles success"
                (spies/reset! source-spy action-spy dispatch-spy nav-spy)
                (on-success {:simulator {:id ::id}})

                (is (spies/called-with? nav-spy :details {:id ::id}))))))

        (testing "when form is not submittable"
          (spies/reset! prevent-spy request-spy)
          (spies/respond-with! error-spy (constantly ::errors))
          ((shared.interactions/create-simulator ::form source-spy) ::event)

          (testing "prevents default behavior"
            (is (spies/called-with? prevent-spy ::event)))

          (testing "does not make a request"
            (is (spies/never-called? request-spy))))))))

(deftest ^:unit show-delete-modal-test
  (testing "(show-delete-modal)"
    (let [action-spy (spies/constantly ::action)
          delete-spy (spies/constantly ::delete)
          delete-handler (spies/constantly delete-spy)
          dispatch-spy (spies/create)]
      (with-redefs [actions/show-modal action-spy
                    shared.interactions/delete-sim delete-handler
                    store/dispatch dispatch-spy]
        (testing "when taking the action"
          ((shared.interactions/show-delete-modal ::id) ::event)
          (testing "shows a modal"
            (is (spies/called-with? action-spy
                                    [:modals/confirm-delete "this simulator"]
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
              (is (test.dom/contains? close-btn "Cancel")))

            (testing "gives the modal a button which deletes the simulator"
              (let [f (:on-click (test.dom/attrs delete-btn))]
                (is (spies/called-with? delete-handler ::id))
                (f ::hide)
                (is (spies/called-with? delete-spy ::hide))))))))))

(deftest ^:unit show-request-modal-test
  (testing "(show-request-modal)"
    (let [action-spy (spies/constantly ::action)
          dispatch-spy (spies/create)]
      (with-redefs [actions/show-modal action-spy
                    store/dispatch dispatch-spy]
        (testing "when making the request"
          ((shared.interactions/show-request-modal ::sim {::some ::request :timestamp ::dt}) ::event)
          (testing "dispatches an action"
            (is (spies/called-with? action-spy
                                    [:modals/request-modal ::sim {::some ::request :timestamp ::dt}]
                                    "Request Details"))
            (is (spies/called-with? dispatch-spy ::action))))))))

(defn run-tests []
  (t/run-tests))
