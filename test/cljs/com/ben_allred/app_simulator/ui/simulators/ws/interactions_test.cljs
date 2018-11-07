(ns com.ben-allred.app-simulator.ui.simulators.ws.interactions-test
  (:require
    [clojure.test :as t :refer [deftest is testing]]
    [com.ben-allred.app-simulator.templates.resources.ws :as resources]
    [com.ben-allred.app-simulator.templates.transformations.ws :as tr]
    [com.ben-allred.app-simulator.templates.views.forms.shared :as shared.views]
    [com.ben-allred.app-simulator.ui.services.forms.core :as forms]
    [com.ben-allred.app-simulator.ui.services.store.actions :as actions]
    [com.ben-allred.app-simulator.ui.services.store.core :as store]
    [com.ben-allred.app-simulator.ui.simulators.shared.interactions :as shared.interactions]
    [com.ben-allred.app-simulator.ui.simulators.ws.interactions :as interactions]
    [test.utils.dom :as test.dom]
    [test.utils.spies :as spies]))

(deftest ^:unit update-simulator-test
  (testing "(update-simulator)"
    (with-redefs [shared.interactions/update-simulator (spies/constantly ::update)]
      (testing "updates the simulator"
        (let [handler (interactions/update-simulator ::form ::id)]
          (is (spies/called-with? shared.interactions/update-simulator ::form tr/model->source ::id))
          (is (= ::update handler)))))))

(deftest ^:unit reset-simulator-test
  (testing "(reset-simulator)"
    (with-redefs [shared.interactions/reset-config (spies/constantly ::reset)]
      (testing "resets the simulator"
        (let [handler (interactions/reset-simulator ::form ::id)]
          (is (spies/called-with? shared.interactions/reset-config ::form tr/sim->model ::id :ws))
          (is (= ::reset handler)))))))

(deftest ^:unit create-simulator-test
  (testing "(create-simulator)"
    (with-redefs [shared.interactions/create-simulator (spies/constantly ::create)]
      (testing "creates the simulator"
        (let [handler (interactions/create-simulator ::form)]
          (is (spies/called-with? shared.interactions/create-simulator ::form tr/model->source))
          (is (= ::create handler)))))))

(deftest ^:unit disconnect-all-test
  (testing "(disconnect-all)"
    (with-redefs [actions/disconnect-all (spies/constantly ::action)
                  store/dispatch (spies/constantly ::dispatch)
                  shared.interactions/toaster (spies/create (fn [level _] level))
                  shared.interactions/do-request (spies/create)]
      (testing "disconnects all sockets"
        ((interactions/disconnect-all ::id) ::event)
        (is (spies/called-with? actions/disconnect-all ::id))
        (is (spies/called-with? store/dispatch ::action))
        (is (spies/called-with? shared.interactions/toaster :success (spies/matcher string?)))
        (is (spies/called-with? shared.interactions/toaster :error (spies/matcher string?)))
        (is (spies/called-with? shared.interactions/do-request ::dispatch :success :error))))))

(deftest ^:unit disconnect-test
  (testing "(disconnect)"
    (with-redefs [actions/disconnect (spies/constantly ::action)
                  store/dispatch (spies/constantly ::dispatch)
                  shared.interactions/toaster (spies/create (fn [level _] level))
                  shared.interactions/do-request (spies/create)]
      (testing "disconnects the specified sockets"
        ((interactions/disconnect ::simulator-id ::socket-id) ::event)
        (is (spies/called-with? actions/disconnect ::simulator-id ::socket-id))
        (is (spies/called-with? store/dispatch ::action))
        (is (spies/called-with? shared.interactions/toaster :success (spies/matcher string?)))
        (is (spies/called-with? shared.interactions/toaster :error (spies/matcher string?)))
        (is (spies/called-with? shared.interactions/do-request ::dispatch :success :error))))))

(deftest ^:unit send-message-test
  (testing "(send-message)"
    (let [hide-spy (spies/create)]
      (with-redefs [forms/syncing? (constantly ::syncing)
                    shared.interactions/resetter (constantly identity)
                    shared.interactions/do-request (spies/constantly ::request)
                    store/dispatch (spies/constantly ::dispatch)
                    shared.interactions/toaster (spies/create (fn [level _] (constantly level)))
                    actions/send-message (spies/constantly ::action)
                    actions/show-toast (constantly ::toast)
                    forms/verify! (constantly nil)
                    forms/errors (constantly nil)
                    forms/current-model (constantly {:message ::message})]
        (testing "handles the request"
          (((interactions/send-message ::form ::simulator-id ::socket-id) hide-spy) ::event)
          (is (spies/called-with? actions/send-message ::simulator-id ::socket-id ::message))
          (is (spies/called-with? store/dispatch ::action))
          (is (spies/called-with? shared.interactions/toaster :success (spies/matcher string?)))
          (is (spies/called-with? shared.interactions/toaster :error (spies/matcher string?)))
          (let [[dispatch on-success on-error] (first (spies/calls shared.interactions/do-request))]
            (is (= dispatch ::dispatch))
            (on-success ::result)
            (is (spies/called? hide-spy))
            (is (= :error (on-error ::result)))))))))

(deftest ^:unit show-send-modal-test
  (testing "(show-send-modal)"
    (with-redefs [forms/display-errors (constantly nil)
                  forms/current-model (constantly {:message ::message})
                  forms/create (spies/constantly ::form)
                  actions/show-modal (spies/constantly ::action)
                  store/dispatch (spies/create)
                  interactions/send-message (spies/constantly ::send)]
      (let [handler (interactions/show-send-modal ::simulator-id ::socket-id)]
        (handler ::event)
        (testing "creates a form"
          (is (spies/called-with? forms/create {} resources/socket-message)))

        (testing "shows the modal"
          (is (spies/called-with? actions/show-modal
                                  [:modals/message-editor ::form nil resources/view->model]
                                  (spies/matcher string?)
                                  (spies/matcher vector?)
                                  (spies/matcher vector?)))
          (is (spies/called-with? store/dispatch ::action)))

        (let [tree (->> actions/show-modal
                        (spies/calls)
                        (first)
                        (filter vector?)
                        (into [:div]))
              send-button (test.dom/query-one tree shared.views/sync-button :.send-button)
              cancel-button (test.dom/query-one tree shared.views/sync-button :.cancel-button)]
          (testing "has a cancel button"
            (is cancel-button))

          (testing "has a send button"
            (is (-> send-button
                    (test.dom/attrs)
                    (:disabled)
                    (not)))
            (is (spies/called-with? interactions/send-message ::form ::simulator-id ::socket-id))
            (is (-> send-button
                    (test.dom/attrs)
                    (:on-click)
                    (= ::send)))))))))

(deftest ^:unit show-ws-modal-test
  (testing "(show-ws-modal)"
    (with-redefs [store/dispatch (spies/create)
                  actions/show-modal (spies/constantly ::action)]
      (testing "shows the socket modal"
        ((interactions/show-ws-modal ::message) ::ignored)

        (is (spies/called-with? actions/show-modal [:modals/socket-modal ::message] (spies/matcher string?)))
        (is (spies/called-with? store/dispatch ::action))))))

(defn run-tests []
  (t/run-tests))
