(ns com.ben-allred.app-simulator.ui.simulators.file.interactions-test
  (:require
    [clojure.test :as t :refer [deftest is testing]]
    [com.ben-allred.app-simulator.services.forms.core :as forms]
    [com.ben-allred.app-simulator.templates.transformations.file :as tr]
    [com.ben-allred.app-simulator.ui.services.store.actions :as actions]
    [com.ben-allred.app-simulator.ui.services.store.core :as store]
    [com.ben-allred.app-simulator.ui.simulators.file.interactions :as interactions]
    [com.ben-allred.app-simulator.ui.simulators.shared.interactions :as shared.interactions]
    [com.ben-allred.app-simulator.utils.chans :as ch]
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
                  ch/then (spies/constantly ::then'd)
                  ch/catch (spies/constantly ::catch'd)
                  ch/finally (spies/create)
                  shared.interactions/toast (spies/create)
                  actions/show-modal (spies/constantly ::action)]
      ((interactions/show-delete-modal ::title ::msg ::click) ::ignored)
      (let [hide-spy (spies/create)
            [modal title delete-btn cancel-btn] (first (spies/calls actions/show-modal))]
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
                [_ then-f] (first (spies/calls ch/then))
                [_ catch-f] (first (spies/calls ch/catch))
                [_ finally-f] (first (spies/calls ch/finally))]

            (is (spies/called-with? store/dispatch ::click))
            (is (spies/called-with? ch/then ::request (spies/matcher fn?)))
            (is (spies/called-with? ch/catch ::then'd (spies/matcher fn?)))
            (is (spies/called-with? ch/finally ::catch'd (spies/matcher fn?)))
            (then-f ::success)
            (is (spies/called-with? shared.interactions/toast ::success :success (spies/matcher string?)))
            (catch-f ::error)
            (is (spies/called-with? shared.interactions/toast ::error :error (spies/matcher string?)))
            (finally-f)
            (is (spies/called? hide-spy))))

        (testing "has a cancel button"
          (is (test.dom/query-one cancel-btn :.cancel-button)))))))

(deftest ^:unit replace-resource-test
  (testing "(replace-resource)"
    (with-redefs [actions/upload-replace (spies/constantly ::action)
                  store/dispatch (spies/constantly ::request)
                  shared.interactions/toast (spies/create)
                  ch/then (spies/constantly ::then'd)
                  ch/catch (spies/constantly ::catch'd)]
      (let [reset-spy (spies/create)
            form (reify
                   IDeref
                   (-deref [_]
                     ::model)
                   IReset
                   (-reset! [_ model]
                     (reset-spy model)))]
        (testing "replaces a resource"
          ((interactions/replace-resource form ::id) ::files)
          (is (spies/called-with? actions/upload-replace ::id ::files))
          (is (spies/called-with? store/dispatch ::action))
          (is (spies/called-with? ch/then ::request (spies/matcher fn?)))
          (is (spies/called-with? ch/catch ::then'd (spies/matcher fn?)))

          (let [[_ on-success] (first (spies/calls ch/then))
                [_ on-error] (first (spies/calls ch/catch))]
            (testing "handles a success response"
              (spies/reset! shared.interactions/toast reset-spy)
              (on-success ::body)
              (is (spies/called-with? shared.interactions/toast ::body :success (spies/matcher string?)))
              (is (spies/called-with? reset-spy ::model)))

            (testing "handles an error response"
              (spies/reset! shared.interactions/toast reset-spy)
              (on-error ::body)
              (is (spies/called-with? shared.interactions/toast ::body :error (spies/matcher string?))))))))))

(deftest ^:unit upload-resources-test
  (testing "(upload-resources)"
    (with-redefs [actions/upload (spies/constantly ::action)
                  store/dispatch (spies/constantly ::request)
                  shared.interactions/toast (spies/create)
                  ch/then (spies/constantly ::then'd)
                  ch/catch (spies/constantly ::catch'd)]
      (let [reset-spy (spies/create)
            form (reify
                   IDeref
                   (-deref [_]
                     ::model)
                   IReset
                   (-reset! [_ model]
                     (reset-spy model)))]
        (testing "replaces a resource"
          ((interactions/upload-resources form) ::files)
          (is (spies/called-with? actions/upload ::files))
          (is (spies/called-with? store/dispatch ::action))
          (is (spies/called-with? ch/then ::request (spies/matcher fn?)))
          (is (spies/called-with? ch/catch ::then'd (spies/matcher fn?)))

          (let [[_ on-success] (first (spies/calls ch/then))
                [_ on-error] (first (spies/calls ch/catch))]
            (testing "handles a success response"
              (spies/reset! shared.interactions/toast reset-spy)
              (on-success ::body)
              (is (spies/called-with? shared.interactions/toast ::body :success (spies/matcher string?)))
              (is (spies/called-with? reset-spy ::model)))

            (testing "handles an error response"
              (spies/reset! shared.interactions/toast reset-spy)
              (on-error ::body)
              (is (spies/called-with? shared.interactions/toast ::body :error (spies/matcher string?))))))))))

(defn run-tests []
  (t/run-tests))
