(ns com.ben-allred.clj-app-simulator.ui.simulators.file.interactions-test
  (:require
    [clojure.test :as t :refer [deftest is testing]]
    [com.ben-allred.clj-app-simulator.templates.transformations.file :as tr]
    [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
    [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
    [com.ben-allred.clj-app-simulator.ui.simulators.file.interactions :as interactions]
    [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as shared.interactions]
    [com.ben-allred.clj-app-simulator.ui.simulators.shared.modals :as modals]
    [test.utils.dom :as test.dom]
    [test.utils.spies :as spies]))

(deftest ^:unit update-simulator
  (testing "(update-simulator)"
    (let [shared-spy (spies/constantly ::handler)]
      (with-redefs [shared.interactions/update-simulator shared-spy]
        (testing "updates the simulator"
          (let [handler (interactions/update-simulator ::form ::id)]
            (is (spies/called-with? shared-spy ::form tr/model->source ::id))
            (is (= handler ::handler))))))))

(deftest ^:unit reset-simulator
  (testing "(reset-simulator)"
    (let [shared-spy (spies/constantly ::handler)]
      (with-redefs [shared.interactions/reset-simulator shared-spy]
        (testing "resets the simulator"
          (let [handler (interactions/reset-simulator ::form ::id)]
            (is (spies/called-with? shared-spy ::form tr/sim->model ::id))
            (is (= handler ::handler))))))))

(deftest ^:unit create-simulator
  (testing "(create-simulator)"
    (let [shared-spy (spies/constantly ::handler)]
      (with-redefs [shared.interactions/create-simulator shared-spy]
        (testing "creates the simulator"
          (let [handler (interactions/create-simulator ::form)]
            (is (spies/called-with? shared-spy ::form tr/model->source))
            (is (= handler ::handler))))))))

(deftest ^:unit show-delete-modal-test
  (testing "(show-delete-modal)"
    (let [dispatch-spy (spies/constantly ::request)
          show-spy (spies/constantly ::action)
          request-spy (spies/create)
          toaster-spy (spies/create (fn [level _] (constantly level)))
          hide-spy (spies/create)]
      (with-redefs [store/dispatch dispatch-spy
                    shared.interactions/do-request request-spy
                    shared.interactions/toaster toaster-spy
                    actions/show-modal show-spy]
        ((interactions/show-delete-modal ::title ::msg ::on-click) ::ignored)
        (let [[modal title delete-btn cancel-btn] (first (spies/calls show-spy))]
          (testing "dispatches the action"
            (is (spies/called-with? dispatch-spy ::action)))

          (testing "has a modal"
            (is (= modal [modals/confirm-delete ::msg])))

          (testing "has a title"
            (is (= title ::title)))

          (testing "has a delete button"
            (let [on-click (-> delete-btn
                               (test.dom/query-one :.delete-button)
                               (test.dom/attrs)
                               (:on-click))
                  _ ((on-click hide-spy) ::ignored)
                  [request on-success on-error] (first (spies/calls request-spy))]

              (is (= request ::request))
              (is (spies/called-with? dispatch-spy ::on-click))
              (is (spies/called-with? toaster-spy :success (spies/matcher string?)))
              (is (spies/called-with? toaster-spy :error (spies/matcher string?)))
              (is (= :error (on-error ::ignored)))
              (on-success ::ignored)
              (is (spies/called? hide-spy))))

          (testing "has a cancel button"
            (is (test.dom/query-one cancel-btn :.cancel-button))))))))

(deftest ^:unit replace-resource-test
  (testing "(replace-resource)"
    (let [replace-spy (spies/constantly ::action)
          dispatch-spy (spies/constantly ::request)
          toaster-spy (spies/create (fn [level _] level))
          request-spy (spies/create)]
      (with-redefs [actions/upload-replace replace-spy
                    store/dispatch dispatch-spy
                    shared.interactions/toaster toaster-spy
                    shared.interactions/do-request request-spy]
        (testing "replaces a resource"
          (interactions/replace-resource ::id ::files)
          (is (spies/called-with? replace-spy ::id ::files))
          (is (spies/called-with? dispatch-spy ::action))
          (is (spies/called-with? toaster-spy :success (spies/matcher string?)))
          (is (spies/called-with? toaster-spy :error (spies/matcher string?)))
          (is (spies/called-with? request-spy ::request :success :error)))))))

(deftest ^:unit upload-resources-test
  (testing "(upload-resources)"
    (let [upload-spy (spies/constantly ::action)
          dispatch-spy (spies/constantly ::request)
          toaster-spy (spies/create (fn [level _] level))
          request-spy (spies/create)]
      (with-redefs [actions/upload upload-spy
                    store/dispatch dispatch-spy
                    shared.interactions/toaster toaster-spy
                    shared.interactions/do-request request-spy]
        (testing "uploads resources"
          (interactions/upload-resources ::files)
          (is (spies/called-with? upload-spy ::files))
          (is (spies/called-with? dispatch-spy ::action))
          (is (spies/called-with? toaster-spy :success (spies/matcher string?)))
          (is (spies/called-with? toaster-spy :error (spies/matcher string?)))
          (is (spies/called-with? request-spy ::request :success :error)))))))

(defn run-tests []
  (t/run-tests))
