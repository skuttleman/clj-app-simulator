(ns com.ben-allred.clj-app-simulator.ui.simulators.ws.interactions-test
  (:require
    [clojure.test :as t :refer [deftest is testing]]
    [com.ben-allred.clj-app-simulator.templates.fields :as fields]
    [com.ben-allred.clj-app-simulator.templates.resources.ws :as resources]
    [com.ben-allred.clj-app-simulator.templates.transformations.ws :as tr]
    [com.ben-allred.clj-app-simulator.templates.views.forms.shared :as shared.views]
    [com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]
    [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
    [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
    [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as shared.interactions]
    [com.ben-allred.clj-app-simulator.ui.simulators.shared.modals :as modals]
    [com.ben-allred.clj-app-simulator.ui.simulators.ws.interactions :as interactions]
    [test.utils.dom :as test.dom]
    [test.utils.spies :as spies]))

(deftest ^:unit update-simulator-test
  (testing "(update-simulator)"
    (let [update-spy (spies/constantly ::update)]
      (with-redefs [shared.interactions/update-simulator update-spy]
        (testing "updates the simulator"
          (let [handler (interactions/update-simulator ::form ::id)]
            (is (spies/called-with? update-spy ::form tr/model->source ::id))
            (is (= ::update handler))))))))

(deftest ^:unit reset-simulator-test
  (testing "(reset-simulator)"
    (let [reset-spy (spies/constantly ::reset)]
      (with-redefs [shared.interactions/reset-simulator reset-spy]
        (testing "resets the simulator"
          (let [handler (interactions/reset-simulator ::form ::id)]
            (is (spies/called-with? reset-spy ::form tr/sim->model ::id))
            (is (= ::reset handler))))))))

(deftest ^:unit create-simulator-test
  (testing "(create-simulator)"
    (let [create-spy (spies/constantly ::create)]
      (with-redefs [shared.interactions/create-simulator create-spy]
        (testing "creates the simulator"
          (let [handler (interactions/create-simulator ::form)]
            (is (spies/called-with? create-spy ::form tr/model->source))
            (is (= ::create handler))))))))

(deftest ^:unit disconnect-all-test
  (testing "(disconnect-all)"
    (let [action-spy (spies/constantly ::action)
          dispatch-spy (spies/constantly ::dispatch)
          toaster-spy (spies/create (fn [level _] level))
          request-spy (spies/create)]
      (with-redefs [actions/disconnect-all action-spy
                    store/dispatch dispatch-spy
                    shared.interactions/toaster toaster-spy
                    shared.interactions/do-request request-spy]
        (testing "disconnects all sockets"
          ((interactions/disconnect-all ::id) ::event)
          (is (spies/called-with? action-spy ::id))
          (is (spies/called-with? dispatch-spy ::action))
          (is (spies/called-with? toaster-spy :success (spies/matcher string?)))
          (is (spies/called-with? toaster-spy :error (spies/matcher string?)))
          (is (spies/called-with? request-spy ::dispatch :success :error)))))))

(deftest ^:unit disconnect-test
  (testing "(disconnect)"
    (let [action-spy (spies/constantly ::action)
          dispatch-spy (spies/constantly ::dispatch)
          toaster-spy (spies/create (fn [level _] level))
          request-spy (spies/create)]
      (with-redefs [actions/disconnect action-spy
                    store/dispatch dispatch-spy
                    shared.interactions/toaster toaster-spy
                    shared.interactions/do-request request-spy]
        (testing "disconnects the specified sockets"
          ((interactions/disconnect ::simulator-id ::socket-id) ::event)
          (is (spies/called-with? action-spy ::simulator-id ::socket-id))
          (is (spies/called-with? dispatch-spy ::action))
          (is (spies/called-with? toaster-spy :success (spies/matcher string?)))
          (is (spies/called-with? toaster-spy :error (spies/matcher string?)))
          (is (spies/called-with? request-spy ::dispatch :success :error)))))))

(deftest ^:unit send-message-test
  (testing "(send-message)"
    (let [do-request-spy (spies/constantly ::request)
          dispatch-spy (spies/constantly ::dispatch)
          toaster-spy (spies/create (fn [level _] (constantly level)))
          hide-spy (spies/create)
          action-spy (spies/constantly ::action)
          toast-spy (spies/constantly ::toast)
          verify-spy (spies/create)
          errors-spy (spies/create)
          model-spy (spies/constantly {:message ::message})]
      (with-redefs [shared.interactions/do-request do-request-spy
                    store/dispatch dispatch-spy
                    shared.interactions/toaster toaster-spy
                    actions/send-message action-spy
                    actions/show-toast toast-spy
                    forms/verify! verify-spy
                    forms/errors errors-spy
                    forms/current-model model-spy]
        (testing "handles the request"
          (((interactions/send-message ::form ::simulator-id ::socket-id) hide-spy) ::event)
          (is (spies/called-with? action-spy ::simulator-id ::socket-id ::message))
          (is (spies/called-with? dispatch-spy ::action))
          (is (spies/called-with? toaster-spy :success (spies/matcher string?)))
          (is (spies/called-with? toaster-spy :error (spies/matcher string?)))
          (let [[dispatch on-success on-error] (first (spies/calls do-request-spy))]
            (is (= dispatch ::dispatch))
            (on-success ::result)
            (is (spies/called? hide-spy))
            (is (= :error (on-error ::result)))))))))

(deftest ^:unit message-editor-test
  (testing "(message-editor)"
    (let [with-attrs-spy (spies/create identity)]
      (with-redefs [shared.views/with-attrs with-attrs-spy]
        (let [root (modals/message-editor ::form ::model->view ::view->model)
              input (test.dom/query-one root fields/textarea)]
          (testing "renders a message field"
            (is (spies/called-with? with-attrs-spy (spies/matcher map?) ::form [:message] ::model->view ::view->model))
            (is (= "Message" (:label (test.dom/attrs input))))))))))

(deftest ^:unit show-send-modal-test
  (testing "(show-send-modal)"
    (let [create-spy (spies/constantly ::form)
          action-spy (spies/constantly ::action)
          dispatch-spy (spies/create)
          errors-spy (spies/create)
          send-spy (spies/constantly ::send)
          model-spy (spies/constantly {:message ::message})]
      (with-redefs [forms/create create-spy
                    actions/show-modal action-spy
                    store/dispatch dispatch-spy
                    forms/display-errors errors-spy
                    interactions/send-message send-spy
                    forms/current-model model-spy]
        (let [handler (interactions/show-send-modal ::simulator-id ::socket-id)]
          (handler ::event)
          (testing "creates a form"
            (is (spies/called-with? create-spy {} resources/socket-message)))

          (testing "shows the modal"
            (is (spies/called-with? action-spy
                                    [:modals/message-editor ::form nil resources/view->model]
                                    (spies/matcher string?)
                                    (spies/matcher vector?)
                                    (spies/matcher vector?)))
            (is (spies/called-with? dispatch-spy ::action)))

          (let [tree (->> action-spy
                          (spies/calls)
                          (first)
                          (filter vector?)
                          (into [:div]))
                send-button (test.dom/query-one tree :.button.is-info)
                cancel-button (test.dom/query-one tree :.cancel-button)]
            (testing "has a cancel button"
              (is (test.dom/query-one cancel-button :button)))

            (testing "has a send button"
              (is (-> send-button
                      (test.dom/attrs)
                      (:disabled)
                      (not)))
              (is (spies/called-with? send-spy ::form ::simulator-id ::socket-id))
              (is (-> send-button
                      (test.dom/attrs)
                      (:on-click)
                      (= ::send))))))))))

(deftest ^:unit show-ws-modal-test
  (testing "(show-ws-modal)"
    (let [dispatch-spy (spies/create)
          action-spy (spies/constantly ::action)]
      (with-redefs [store/dispatch dispatch-spy
                    actions/show-modal action-spy]
        (testing "shows the socket modal"
          ((interactions/show-ws-modal ::message) ::ignored)

          (is (spies/called-with? action-spy [:modals/socket-modal ::message] (spies/matcher string?)))
          (is (spies/called-with? dispatch-spy ::action)))))))

(defn run-tests []
  (t/run-tests))
