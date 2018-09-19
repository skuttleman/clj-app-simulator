(ns com.ben-allred.clj-app-simulator.ui.simulators.ws.interactions-test
  (:require [cljs.test :as t :refer [deftest testing is]]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as shared.interactions]
            [com.ben-allred.clj-app-simulator.ui.simulators.ws.interactions :as interactions]
            [com.ben-allred.clj-app-simulator.templates.transformations.ws :as tr]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]
            [com.ben-allred.formation.core :as f]
            [com.ben-allred.clj-app-simulator.templates.resources.ws :as resources]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.modals :as modals]
            [test.utils.dom :as test.dom]))

(deftest ^:unit update-simulator-test
  (testing "(update-simulator)"
    (let [update-spy (spies/constantly ::update)]
      (with-redefs [shared.interactions/update-simulator update-spy]
        (testing "updates the simulator"
          (let [handler (interactions/update-simulator ::form ::id ::submittable?)]
            (is (spies/called-with? update-spy ::form tr/model->source ::id ::submittable?))
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
          (let [handler (interactions/create-simulator ::form ::submittable?)]
            (is (spies/called-with? create-spy ::form tr/model->source ::submittable?))
            (is (= ::create handler))))))))

(deftest ^:unit disconnect-all-test
  (testing "(disconnect-all)"
    (let [action-spy (spies/constantly ::action)
          dispatch-spy (spies/constantly ::dispatch)
          request-spy (spies/create)]
      (with-redefs [actions/disconnect-all action-spy
                    store/dispatch dispatch-spy
                    shared.interactions/do-request request-spy]
        (testing "disconnects all sockets"
          ((interactions/disconnect-all ::id) ::event)
          (is (spies/called-with? action-spy ::id))
          (is (spies/called-with? dispatch-spy ::action))
          (is (spies/called-with? request-spy ::dispatch)))))))

(deftest ^:unit disconnect-test
  (testing "(disconnect)"
    (let [action-spy (spies/constantly ::action)
          dispatch-spy (spies/constantly ::dispatch)
          request-spy (spies/create)]
      (with-redefs [actions/disconnect action-spy
                    store/dispatch dispatch-spy
                    shared.interactions/do-request request-spy]
        (testing "disconnects all sockets"
          ((interactions/disconnect ::simulator-id ::socket-id) ::event)
          (is (spies/called-with? action-spy ::simulator-id ::socket-id))
          (is (spies/called-with? dispatch-spy ::action))
          (is (spies/called-with? request-spy ::dispatch)))))))

(deftest ^:unit send-message-test
  (testing "(send-message)"
    (let [do-request-spy (spies/constantly ::request)
          dispatch-spy (spies/constantly ::dispatch)
          action-spy (spies/constantly ::action)]
      (with-redefs [shared.interactions/do-request do-request-spy
                    store/dispatch dispatch-spy
                    actions/send-message action-spy]
        (testing "handles the request"
          ((interactions/send-message ::simulator-id ::socket-id ::message ::hide) ::event)
          (is (spies/called-with? action-spy ::simulator-id ::socket-id ::message))
          (is (spies/called-with? dispatch-spy ::action))
          (is (spies/called-with? do-request-spy ::dispatch ::hide)))))))

(deftest ^:unit send-message-button-test
  (testing "(send-message-button)"
    (let [errors-spy (spies/constantly ::errors)]
      (with-redefs [forms/errors errors-spy]
        (testing "renders a button"
          (let [button (interactions/send-message-button {::some ::attrs} ::form)
                attrs (test.dom/attrs button)]
            (is (spies/called-with? errors-spy ::form))
            (is (= ::attrs (::some attrs)))
            (is (= ::errors (:disabled attrs)))))))))

(deftest ^:unit show-message-modal-test
  (testing "(show-message-modal)"
    (let [create-spy (spies/constantly ::form)
          action-spy (spies/constantly ::action)
          dispatch-spy (spies/create)
          errors-spy (spies/create)
          send-spy (spies/constantly ::send)
          model-spy (spies/constantly {:message ::message})]
      (with-redefs [forms/create create-spy
                    actions/show-modal action-spy
                    store/dispatch dispatch-spy
                    forms/errors errors-spy
                    interactions/send-message send-spy
                    forms/current-model model-spy]
        (let [handler (interactions/show-message-modal ::simulator-id ::socket-id)]
          (handler ::event)
          (testing "creates a form"
            (is (spies/called-with? create-spy {:message ""} resources/socket-message)))

          (testing "shows the modal"
            (is (spies/called-with? action-spy
                                    [modals/message ::form nil nil]
                                    (spies/matcher string?)
                                    (spies/matcher vector?)
                                    (spies/matcher vector?)))
            (is (spies/called-with? dispatch-spy ::action)))

          (let [tree (->> action-spy
                          (spies/calls)
                          (first)
                          (filter vector?)
                          (into [:div]))
                send-button (test.dom/query-one tree interactions/send-message-button)
                cancel-button (test.dom/query-one tree :.cancel-button)
                on-click (:on-click (test.dom/attrs send-button))]
            (testing "has a cancel button"
              (is (test.dom/query-one cancel-button :button)))

            (testing "has a send button"
              (is (-> send-button
                      (test.dom/attrs)
                      (:disabled)
                      (not)))
              (spies/reset! send-spy model-spy)
              (let [result (on-click ::hide)]
                (is (spies/called-with? model-spy ::form))
                (is (spies/called-with? send-spy ::simulator-id ::socket-id ::message ::hide))
                (is (= ::send result))))))))))

(defn run-tests [] (t/run-tests))
