(ns com.ben-allred.app-simulator.ui.simulators.file.interactions-test
  (:require
    [clojure.test :as t :refer [deftest is testing]]
    [com.ben-allred.app-simulator.templates.transformations.file :as tr]
    [com.ben-allred.app-simulator.ui.services.forms.core :as forms]
    [com.ben-allred.app-simulator.ui.services.store.actions :as actions]
    [com.ben-allred.app-simulator.ui.services.store.core :as store]
    [com.ben-allred.app-simulator.ui.simulators.file.interactions :as interactions]
    [com.ben-allred.app-simulator.ui.simulators.shared.interactions :as shared.interactions]
    [test.utils.dom :as test.dom]
    [test.utils.spies :as spies]))

(deftest ^:unit update-simulator
  (testing "(update-simulator)"
    (with-redefs [shared.interactions/update-simulator (spies/constantly ::handler)]
      (testing "updates the simulator"
        (let [handler (interactions/update-simulator ::form ::id)]
          (is (spies/called-with? shared.interactions/update-simulator ::form tr/model->source ::id))
          (is (= handler ::handler)))))))

(deftest ^:unit reset-simulator
  (testing "(reset-simulator)"
    (with-redefs [shared.interactions/reset-config (spies/constantly ::handler)]
      (testing "resets the simulator"
        (let [handler (interactions/reset-simulator ::form ::id)]
          (is (spies/called-with? shared.interactions/reset-config ::form tr/sim->model ::id :file))
          (is (= handler ::handler)))))))

(deftest ^:unit create-simulator
  (testing "(create-simulator)"
    (with-redefs [shared.interactions/create-simulator (spies/constantly ::handler)]
      (testing "creates the simulator"
        (let [handler (interactions/create-simulator ::form)]
          (is (spies/called-with? shared.interactions/create-simulator ::form tr/model->source))
          (is (= handler ::handler)))))))

(deftest ^:unit show-delete-modal-test
  (testing "(show-delete-modal)"
    (with-redefs [store/dispatch (spies/constantly ::request)
                  shared.interactions/do-request (spies/create)
                  shared.interactions/toaster (spies/create (fn [level _] (constantly level)))
                  actions/show-modal (spies/constantly ::action)]
      ((interactions/show-delete-modal ::title ::msg ::on-click) ::ignored)
      (let [hide-spy (spies/create)
            [modal title delete-btn cancel-btn] (first (spies/calls actions/show-modalspy))]
        (testing "dispatches the action"
          (is (spies/called-with? store/dispatch ::action)))

        (testing "has a modal"
          (is (= modal [:modals/confirm-delete ::msg])))

        (testing "has a title"
          (is (= title ::title)))

        (testing "has a delete button"
          (let [on-click (-> delete-btn
                             (test.dom/query-one :.delete-button)
                             (test.dom/attrs)
                             (:on-click))
                _ ((on-click hide-spy) ::ignored)
                [request on-success on-error] (first (spies/calls shared.interactions/do-request))]

            (is (= request ::request))
            (is (spies/called-with? store/dispatch ::on-click))
            (is (spies/called-with? shared.interactions/toaster :success (spies/matcher string?)))
            (is (spies/called-with? shared.interactions/toaster :error (spies/matcher string?)))
            (is (= :error (on-error ::ignored)))
            (on-success ::ignored)
            (is (spies/called? hide-spy))))

        (testing "has a cancel button"
          (is (test.dom/query-one cancel-btn :.cancel-button)))))))

(deftest ^:unit replace-resource-test
  (testing "(replace-resource)"
    (with-redefs [actions/upload-replace (spies/constantly ::action)
                  store/dispatch (spies/constantly ::request)
                  shared.interactions/toaster (spies/create (fn [level _] level))
                  shared.interactions/resetter (spies/create (fn [f & _] f))
                  shared.interactions/do-request (spies/create)
                  forms/current-model (spies/constantly ::model)
                  forms/sync! (spies/create)
                  forms/reset! :reset!
                  forms/ready! :ready!]
      (testing "replaces a resource"
        ((interactions/replace-resource ::form ::id) ::files)
        (is (spies/called-with? forms/current-model ::form))
        (is (spies/called-with? forms/sync! ::form (spies/matcher any?)))
        (is (spies/called-with? actions/upload-replace ::id ::files))
        (is (spies/called-with? store/dispatch ::action))
        (is (spies/called-with? shared.interactions/resetter :reset! ::form ::model))
        (is (spies/called-with? shared.interactions/resetter :ready! ::form))
        (is (spies/called-with? shared.interactions/toaster :error (spies/matcher string?)))
        (is (spies/called-with? shared.interactions/toaster :success (spies/matcher string?)))
        (let [[request success-fn error-fn] (first (spies/calls shared.interactions/do-request))]
          (is (= ::request request))
          (is (= ::reset (success-fn {:success {:reset! ::reset}})))
          (is (= ::ready (error-fn {:error {:ready! ::ready}}))))))))

(deftest ^:unit upload-resources-test
  (testing "(upload-resources)"
    (with-redefs [actions/upload (spies/constantly ::action)
                  store/dispatch (spies/constantly ::request)
                  shared.interactions/toaster (spies/create (fn [level _] level))
                  shared.interactions/resetter (spies/create (fn [f & _] f))
                  shared.interactions/do-request (spies/create)
                  forms/current-model (spies/constantly ::model)
                  forms/sync! (spies/create)
                  forms/reset! :reset!
                  forms/ready! :ready!]
      (testing "replaces a resource"
        ((interactions/upload-resources ::form) ::files)
        (is (spies/called-with? forms/current-model ::form))
        (is (spies/called-with? forms/sync! ::form (spies/matcher any?)))
        (is (spies/called-with? actions/upload ::files))
        (is (spies/called-with? store/dispatch ::action))
        (is (spies/called-with? shared.interactions/resetter :reset! ::form ::model))
        (is (spies/called-with? shared.interactions/resetter :ready! ::form))
        (is (spies/called-with? shared.interactions/toaster :error (spies/matcher string?)))
        (is (spies/called-with? shared.interactions/toaster :success (spies/matcher string?)))
        (let [[request success-fn error-fn] (first (spies/calls shared.interactions/do-request))]
          (is (= ::request request))
          (is (= ::reset (success-fn {:success {:reset! ::reset}})))
          (is (= ::ready (error-fn {:error {:ready! ::ready}}))))))))

(defn run-tests []
  (t/run-tests))
