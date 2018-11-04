(ns com.ben-allred.app-simulator.ui.simulators.shared.interactions-test
  (:require
    [cljs.core.async :as async]
    [clojure.test :as t :refer [async deftest is testing]]
    [com.ben-allred.app-simulator.ui.services.forms.core :as forms]
    [com.ben-allred.app-simulator.ui.services.navigation :as nav]
    [com.ben-allred.app-simulator.ui.services.store.actions :as actions]
    [com.ben-allred.app-simulator.ui.services.store.core :as store]
    [com.ben-allred.app-simulator.ui.simulators.shared.interactions :as shared.interactions]
    [com.ben-allred.app-simulator.ui.utils.dom :as dom]
    [test.utils.dom :as test.dom]
    [test.utils.spies :as spies]))

(deftest ^:unit toaster-test
  (testing "(toaster)"
    (with-redefs [actions/show-toast (spies/constantly ::action)
                  store/dispatch (spies/create)]
      (let [toast (shared.interactions/toaster ::level ::default-message)]
        (testing "returns a function that returns the body"
          (is (= ::body (toast ::body))))

        (testing "when the body has a message"
          (spies/reset! actions/show-toast store/dispatch)
          (toast {:message ::some-message})

          (testing "toasts the message"
            (is (spies/called-with? actions/show-toast ::level ::some-message))
            (is (spies/called-with? store/dispatch ::action))))

        (testing "when the body has no message"
          (spies/reset! actions/show-toast store/dispatch)
          (toast {})

          (testing "toasts the default message"
            (is (spies/called-with? actions/show-toast ::level ::default-message))
            (is (spies/called-with? store/dispatch ::action))))))))

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
    (with-redefs [forms/current-model (constantly ::model)
                  forms/verify! (constantly nil)
                  forms/changed? (constantly true)
                  dom/prevent-default (spies/create)
                  actions/update-simulator (spies/constantly ::action)
                  store/dispatch (spies/constantly ::dispatch)
                  forms/reset! (spies/create)
                  forms/errors (spies/create)
                  shared.interactions/do-request (spies/create)]
      (let [source-spy (spies/constantly ::source)]
        (testing "when form is submittable"
          (spies/reset! dom/prevent-default shared.interactions/do-request source-spy actions/update-simulator store/dispatch forms/reset!)
          ((shared.interactions/update-simulator ::form source-spy ::id) ::event)

          (testing "prevents default behavior"
            (is (spies/called-with? dom/prevent-default ::event)))

          (testing "and when making the request"
            (let [[request on-success] (first (spies/calls shared.interactions/do-request))]
              (testing "dispatches an action"

                (is (spies/called-with? source-spy ::model))
                (is (spies/called-with? actions/update-simulator ::id ::source))
                (is (spies/called-with? store/dispatch ::action))
                (is (= ::dispatch request)))

              (testing "handles success"
                (spies/reset! source-spy actions/update-simulator store/dispatch forms/reset!)
                (on-success ::ignored)

                (is (spies/called-with? forms/reset! ::form ::model))))))

        (testing "when form has errors"
          (spies/reset! dom/prevent-default shared.interactions/do-request)
          (spies/respond-with! forms/errors (constantly ::errors))
          ((shared.interactions/update-simulator ::form source-spy ::id) ::event)

          (testing "prevents default behavior"
            (is (spies/called-with? dom/prevent-default ::event)))

          (testing "does not make a request"
            (is (spies/never-called? shared.interactions/do-request))))))))

(deftest ^:unit clear-requests-test
  (testing "(clear-requests)"
    (with-redefs [actions/clear-requests (spies/constantly ::action)
                  store/dispatch (spies/constantly ::dispatch)
                  shared.interactions/do-request (spies/create)]
      (testing "when making the request with type :http"
        (spies/reset! actions/clear-requests store/dispatch)
        ((shared.interactions/clear-requests :http ::id) ::event)
        (let [[request] (first (spies/calls shared.interactions/do-request))]
          (testing "dispatches an action"
            (is (spies/called-with? actions/clear-requests ::id :http/requests))
            (is (spies/called-with? store/dispatch ::action))
            (is (= ::dispatch request)))))

      (testing "when making the request with type :ws"
        (spies/reset! actions/clear-requests store/dispatch)
        ((shared.interactions/clear-requests :ws ::id) ::event)
        (let [[request] (first (spies/calls shared.interactions/do-request))]
          (testing "dispatches an action"
            (is (spies/called-with? actions/clear-requests ::id :ws/requests))
            (is (spies/called-with? store/dispatch ::action))
            (is (= ::dispatch request))))))))

(deftest ^:unit delete-sim-test
  (testing "(delete-sim)"
    (let [hide-spy (spies/create)]
      (with-redefs [actions/delete-simulator (spies/constantly ::action)
                    store/dispatch (spies/constantly ::dispatch)
                    shared.interactions/do-request (spies/create)
                    nav/navigate! (spies/create)]
        (testing "when making the request"
          (spies/reset! actions/delete-simulator store/dispatch hide-spy)
          (((shared.interactions/delete-sim ::id) hide-spy) ::event)
          (let [[request on-success] (first (spies/calls shared.interactions/do-request))]
            (testing "deletes the simulator"
              (is (spies/called-with? actions/delete-simulator ::id))
              (is (spies/called-with? store/dispatch ::action))
              (is (= ::dispatch request)))

            (testing "handles success"
              (spies/reset! hide-spy nav/navigate!)
              (on-success ::ignored)
              (is (spies/called? hide-spy))
              (is (spies/called-with? nav/navigate! :home)))))))))

(deftest ^:unit reset-config-test
  (testing "(reset-config)"
    (let [model-spy (spies/constantly ::model)]
      (with-redefs [actions/reset-simulator-config (spies/constantly ::action)
                    store/dispatch (spies/constantly ::dispatch)
                    forms/reset! (spies/create)
                    shared.interactions/do-request (spies/create)]
        (testing "when making the request"
          (spies/reset! actions/reset-simulator-config store/dispatch forms/reset! model-spy)
          ((shared.interactions/reset-config ::form model-spy ::id ::type) ::event)
          (let [[request on-success] (first (spies/calls shared.interactions/do-request))]
            (testing "dispatches an action"

              (is (spies/called-with? actions/reset-simulator-config ::id ::type))
              (is (spies/called-with? store/dispatch ::action))
              (is (= ::dispatch request)))

            (testing "handles success"
              (spies/reset! actions/reset-simulator-config store/dispatch forms/reset! model-spy)
              (on-success {:simulator ::response})

              (is (spies/called-with? model-spy ::response))
              (is (spies/called-with? forms/reset! ::form ::model)))))))))

(deftest ^:unit create-simulator-test
  (testing "(create-simulator)"
    (let [source-spy (spies/constantly ::source)]
      (with-redefs [forms/current-model (constantly ::model)
                    forms/verify! (constantly nil)
                    forms/errors (spies/constantly nil)
                    dom/prevent-default (spies/create)
                    actions/create-simulator (spies/constantly ::action)
                    store/dispatch (spies/constantly ::dispatch)
                    nav/nav-and-replace! (spies/create)
                    shared.interactions/do-request (spies/create)
                    shared.interactions/resetter (spies/create (constantly identity))]
        (testing "when form is submittable"
          (spies/reset! dom/prevent-default shared.interactions/do-request source-spy
                        actions/create-simulator store/dispatch nav/nav-and-replace!)
          ((shared.interactions/create-simulator ::form source-spy) ::event)

          (testing "prevents default behavior"
            (is (spies/called-with? dom/prevent-default ::event)))

          (testing "and when making the request"
            (let [[request on-success] (first (spies/calls shared.interactions/do-request))]
              (testing "dispatches an action"

                (is (spies/called-with? source-spy ::model))
                (is (spies/called-with? actions/create-simulator ::source))
                (is (spies/called-with? store/dispatch ::action))
                (is (spies/called-with? shared.interactions/resetter forms/reset! ::form ::model))
                (is (spies/called-with? shared.interactions/resetter forms/ready! ::form))
                (is (= ::dispatch request)))

              (testing "handles success"
                (spies/reset! source-spy actions/create-simulator store/dispatch nav/nav-and-replace!)
                (on-success {:simulator {:id ::id}})

                (is (spies/called-with? nav/nav-and-replace! :details {:id ::id}))))))

        (testing "when form is not submittable"
          (spies/reset! dom/prevent-default shared.interactions/do-request)
          (spies/respond-with! forms/errors (constantly ::errors))
          ((shared.interactions/create-simulator ::form source-spy) ::event)

          (testing "prevents default behavior"
            (is (spies/called-with? dom/prevent-default ::event)))

          (testing "does not make a request"
            (is (spies/never-called? shared.interactions/do-request))))))))

(deftest ^:unit show-delete-modal-test
  (testing "(show-delete-modal)"
    (let [delete-spy (spies/constantly ::delete)]
      (with-redefs [actions/show-modal (spies/constantly ::action)
                    shared.interactions/delete-sim (spies/constantly delete-spy)
                    store/dispatch (spies/create)]
        (testing "when taking the action"
          ((shared.interactions/show-delete-modal ::id) ::event)
          (testing "shows a modal"
            (is (spies/called-with? actions/show-modal
                                    [:modals/confirm-delete "this simulator"]
                                    "Delete Simulator"
                                    (spies/matcher vector?)
                                    (spies/matcher vector?)))
            (is (spies/called-with? store/dispatch ::action)))

          (let [tree (->> actions/show-modal
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
                (is (spies/called-with? shared.interactions/delete-sim ::id))
                (f ::hide)
                (is (spies/called-with? delete-spy ::hide))))))))))

(deftest ^:unit show-request-modal-test
  (testing "(show-request-modal)"
    (with-redefs [actions/show-modal (spies/constantly ::action)
                  store/dispatch (spies/create)]
      (testing "when making the request"
        ((shared.interactions/show-request-modal ::sim {::some ::request :timestamp ::dt}) ::event)
        (testing "dispatches an action"
          (is (spies/called-with? actions/show-modal
                                  [:modals/request-modal ::sim {::some ::request :timestamp ::dt}]
                                  "Request Details"))
          (is (spies/called-with? store/dispatch ::action)))))))

(defn run-tests []
  (t/run-tests))
