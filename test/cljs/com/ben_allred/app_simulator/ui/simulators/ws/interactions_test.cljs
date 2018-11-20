(ns com.ben-allred.app-simulator.ui.simulators.ws.interactions-test
  (:require
    [clojure.test :as t :refer [deftest is testing]]
    [com.ben-allred.app-simulator.services.forms.core :as forms]
    [com.ben-allred.app-simulator.templates.resources.ws :as resources]
    [com.ben-allred.app-simulator.templates.transformations.ws :as tr]
    [com.ben-allred.app-simulator.templates.views.forms.shared :as shared.views]
    [com.ben-allred.app-simulator.ui.services.forms.standard :as form.std]
    [com.ben-allred.app-simulator.ui.services.store.actions :as actions]
    [com.ben-allred.app-simulator.ui.services.store.core :as store]
    [com.ben-allred.app-simulator.ui.simulators.shared.interactions :as shared.interactions]
    [com.ben-allred.app-simulator.ui.simulators.ws.interactions :as interactions]
    [test.utils.dom :as test.dom]
    [test.utils.spies :as spies]
    [com.ben-allred.app-simulator.utils.chans :as ch]))

(deftest ^:unit update-simulator-test
  (testing "(update-simulator)"
    (with-redefs [shared.interactions/update-simulator (spies/constantly ::update)]
      (testing "updates the simulator"
        (let [handler (interactions/update-simulator ::form ::id)]
          (is (spies/called-with? shared.interactions/update-simulator ::form tr/model->source tr/source->model ::id))
          (is (= ::update handler)))))))

(deftest ^:unit reset-simulator-test
  (testing "(reset-simulator)"
    (with-redefs [shared.interactions/reset-config (spies/constantly ::reset)]
      (testing "resets the simulator"
        (let [handler (interactions/reset-simulator ::id)]
          (is (spies/called-with? shared.interactions/reset-config tr/source->model ::id :ws))
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
    (with-redefs [shared.interactions/toast (spies/create)
                  actions/disconnect-all (spies/constantly ::action)
                  store/dispatch (spies/constantly ::request)
                  ch/peek (spies/constantly ::handled)]
      (let [result ((interactions/disconnect-all ::id) ::event)]
        (testing "disconnects all sockets"
          (is (spies/called-with? actions/disconnect-all ::id))
          (is (spies/called-with? store/dispatch ::action))
          (is (spies/called-with? ch/peek ::request (spies/matcher fn?) (spies/matcher fn?)))
          (is (= result ::handled)))

        (testing "handles a success"
          (let [[_ on-success] (first (spies/calls ch/peek))]
            (spies/reset! shared.interactions/toast)
            (on-success ::body)
            (is (spies/called-with? shared.interactions/toast ::body :success (spies/matcher string?)))))

        (testing "handles an error"
          (let [[_ _ on-error] (first (spies/calls ch/peek))]
            (spies/reset! shared.interactions/toast)
            (on-error ::body)
            (is (spies/called-with? shared.interactions/toast ::body :error (spies/matcher string?)))))))))

(deftest ^:unit disconnect-test
  (testing "(disconnect)"
    (with-redefs [shared.interactions/toast (spies/create)
                  actions/disconnect (spies/constantly ::action)
                  store/dispatch (spies/constantly ::request)
                  ch/peek (spies/constantly ::handled)]
      (let [result ((interactions/disconnect ::sim ::id) ::event)]
        (testing "disconnects all sockets"
          (is (spies/called-with? actions/disconnect ::sim ::id))
          (is (spies/called-with? store/dispatch ::action))
          (is (spies/called-with? ch/peek ::request (spies/matcher fn?) (spies/matcher fn?)))
          (is (= result ::handled)))

        (testing "handles a success"
          (let [[_ on-success] (first (spies/calls ch/peek))]
            (spies/reset! shared.interactions/toast)
            (on-success ::body)
            (is (spies/called-with? shared.interactions/toast ::body :success (spies/matcher string?)))))

        (testing "handles an error"
          (let [[_ _ on-error] (first (spies/calls ch/peek))]
            (spies/reset! shared.interactions/toast)
            (on-error ::body)
            (is (spies/called-with? shared.interactions/toast ::body :error (spies/matcher string?)))))))))

(deftest ^:unit send-message-test
  (testing "(send-message)"
    (with-redefs [shared.interactions/toast (spies/create)
                  actions/send-message (spies/constantly ::action)
                  store/dispatch (spies/constantly ::request)
                  ch/peek (spies/constantly ::peek'd)
                  ch/finally (spies/constantly ::handled)]
      (testing "when the form is creatable"
        (with-redefs [shared.interactions/creatable? (constantly true)]
          (let [hide-spy (spies/create)
                form (reify
                       IDeref
                       (-deref [_]
                         {:message ::message}))
                result (((interactions/send-message form ::sim ::id) hide-spy) ::event)]
            (testing "sends a message"
              (is (spies/called-with? actions/send-message ::sim ::id ::message))
              (is (spies/called-with? store/dispatch ::action))
              (is (spies/called-with? ch/peek ::request (spies/matcher fn?) (spies/matcher fn?)))
              (is (spies/called-with? ch/finally ::peek'd (spies/matcher fn?)))
              (is (= result ::handled)))

            (testing "handles a success"
              (let [[_ on-success] (first (spies/calls ch/peek))]
                (spies/reset! hide-spy shared.interactions/toast)
                (on-success ::body)
                (is (spies/never-called? hide-spy))
                (is (spies/called-with? shared.interactions/toast ::body :success (spies/matcher string?)))))

            (testing "handles an error"
              (let [[_ _ on-error] (first (spies/calls ch/peek))]
                (spies/reset! hide-spy shared.interactions/toast)
                (on-error ::body)
                (is (spies/never-called? hide-spy))
                (is (spies/called-with? shared.interactions/toast ::body :error (spies/matcher string?)))))

            (testing "hides the modal"
              (let [[_ on-finally] (first (spies/calls ch/finally))]
                (spies/reset! hide-spy shared.interactions/toast)
                (on-finally)
                (is (spies/called-with? hide-spy))
                (is (spies/never-called? shared.interactions/toast)))))))

      (testing "when the form is not creatable"
        (with-redefs [shared.interactions/creatable? (constantly false)
                      ch/reject (constantly ::reject'd)]
          (let [result (((interactions/send-message ::form :sim ::id) ::hide) ::event)]
            (is (= ::reject'd result))))))))

(deftest ^:unit show-send-modal-test
  (testing "(show-send-modal)"
    (with-redefs [form.std/create (spies/constantly ::form)
                  actions/show-modal (spies/constantly ::action)
                  store/dispatch (spies/create)
                  interactions/send-message (spies/constantly ::send)]
      (let [handler (interactions/show-send-modal ::simulator-id ::socket-id)]
        (handler ::event)
        (testing "creates a form"
          (is (spies/called-with? form.std/create {} resources/socket-message)))

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
