(ns com.ben-allred.app-simulator.ui.simulators.shared.interactions-test
  (:require
    [clojure.test :as t :refer [deftest is testing]]
    [com.ben-allred.app-simulator.services.forms.core :as forms]
    [com.ben-allred.app-simulator.ui.services.navigation :as nav]
    [com.ben-allred.app-simulator.ui.services.store.actions :as actions]
    [com.ben-allred.app-simulator.ui.services.store.core :as store]
    [com.ben-allred.app-simulator.ui.simulators.shared.interactions :as shared.interactions]
    [com.ben-allred.app-simulator.ui.utils.dom :as dom]
    [test.utils.dom :as test.dom]
    [test.utils.spies :as spies]
    [com.ben-allred.app-simulator.utils.chans :as ch]))

(deftest ^:unit creatable?-test
  (testing "(creatable?)"
    (testing "when the form is valid"
      (with-redefs [forms/valid? (constantly true)]
        (testing "is creatable"
          (is (shared.interactions/creatable? ::form)))))

    (testing "when the form is not valid"
      (with-redefs [forms/valid? (constantly false)]
        (testing "is not creatable"
          (is (not (shared.interactions/creatable? ::form))))))))

(deftest ^:unit updatable?-test
  (testing "(updatable?)"
    (testing "when the form is not creatable"
      (with-redefs [shared.interactions/creatable? (constantly false)
                    forms/changed? (constantly true)]
        (testing "is not updatable"
          (is (not (shared.interactions/updatable? :form))))))

    (testing "when the form in not changed"
      (with-redefs [shared.interactions/creatable? (constantly true)
                    forms/changed? (constantly false)]
        (testing "is not updatable"
          (is (not (shared.interactions/updatable? :form))))))

    (testing "when the form is creatable and changed"
      (with-redefs [shared.interactions/creatable? (constantly true)
                    forms/changed? (constantly true)]
        (testing "is updatable"
          (is (shared.interactions/updatable? :form)))))))

(deftest ^:unit toast-test
  (testing "(toast)"
    (with-redefs [actions/show-toast (spies/constantly ::action)
                  store/dispatch (spies/create)]
      (testing "when the body has a message"
        (spies/reset! actions/show-toast store/dispatch)
        (shared.interactions/toast {:message ::message} ::level ::default-message)
        (testing "shows a toast with the body's message"
          (is (spies/called-with? actions/show-toast ::level ::message))
          (is (spies/called-with? store/dispatch ::action))))

      (testing "when the body does not have a message"
        (testing "shows a toast with the default message"
          (spies/reset! actions/show-toast store/dispatch)
          (shared.interactions/toast {} ::level ::default-message)
          (testing "shows a toast with the default message"
            (is (spies/called-with? actions/show-toast ::level ::default-message))
            (is (spies/called-with? store/dispatch ::action))))))))

(deftest ^:unit update-simulator-test
  (testing "(update-simulator)"
    (testing "when the form is updatable"
      (with-redefs [shared.interactions/updatable? (constantly true)
                    shared.interactions/toast (spies/create)
                    actions/update-simulator (spies/constantly ::action)
                    store/dispatch (spies/constantly ::request)
                    ch/then (spies/create (fn [ch _] ch))
                    ch/catch (spies/constantly ::handled)]
        (let [reset-spy (spies/create)
              form (reify
                     IDeref
                     (-deref [_]
                       ::model)
                     IReset
                     (-reset! [_ value]
                       (reset-spy value)))
              result ((shared.interactions/update-simulator form {::model ::source} ::id) ::event)]
          (testing "updates the simulator"
            (is (spies/called-with? actions/update-simulator ::id ::source))
            (is (spies/called-with? store/dispatch ::action))
            (is (spies/called-with? ch/then ::request (spies/matcher fn?)))
            (is (spies/called-with? ch/catch ::request (spies/matcher fn?)))
            (is (= result ::handled)))

          (testing "handles a success"
            (let [[_ on-success] (first (spies/calls ch/then))]
              (spies/reset! reset-spy shared.interactions/toast)
              (on-success ::body)
              (is (spies/called-with? reset-spy ::model))
              (is (spies/called-with? shared.interactions/toast ::body :success (spies/matcher string?)))))

          (testing "handles an error"
            (let [[_ on-error] (first (spies/calls ch/catch))]
              (spies/reset! reset-spy shared.interactions/toast)
              (on-error ::body)
              (is (spies/never-called? reset-spy))
              (is (spies/called-with? shared.interactions/toast ::body :error (spies/matcher string?))))))))

    (testing "when the form is not updatable"
      (with-redefs [shared.interactions/updatable? (constantly false)
                    ch/reject (constantly ::rejected)]
        (let [form (reify
                     IDeref
                     (-deref [_]))]
          (is (= ::rejected ((shared.interactions/update-simulator form ::model->source ::id) ::event))))))))

(deftest ^:unit clear-requests-test
  (testing "(clear-requests)"
    (with-redefs [shared.interactions/toast (spies/create)
                  actions/clear-requests (spies/constantly ::action)
                  store/dispatch (spies/constantly ::request)
                  ch/then (spies/create (fn [ch _] ch))
                  ch/catch (spies/constantly ::handled)]
      (let [result ((shared.interactions/clear-requests :some-type ::id) ::event)]
        (testing "clears the requests"
          (is (spies/called-with? actions/clear-requests ::id :some-type/requests))
          (is (spies/called-with? store/dispatch ::action))
          (is (spies/called-with? ch/then ::request (spies/matcher fn?)))
          (is (spies/called-with? ch/catch ::request (spies/matcher fn?)))
          (is (= result ::handled)))

        (testing "handles a success"
          (let [[_ on-success] (first (spies/calls ch/then))]
            (spies/reset! shared.interactions/toast)
            (on-success ::body)
            (is (spies/called-with? shared.interactions/toast ::body :success (spies/matcher string?)))))

        (testing "handles an error"
          (let [[_ on-error] (first (spies/calls ch/catch))]
            (spies/reset! shared.interactions/toast)
            (on-error ::body)
            (is (spies/called-with? shared.interactions/toast ::body :error (spies/matcher string?)))))))))

(deftest ^:unit delete-sim-test
  (testing "(delete-sim)"
    (with-redefs [shared.interactions/toast (spies/create)
                  actions/delete-simulator (spies/constantly ::action)
                  store/dispatch (spies/constantly ::request)
                  ch/then (spies/create (fn [ch _] ch))
                  ch/catch (spies/create (fn [ch _] ch))
                  ch/finally (spies/constantly ::handled)
                  nav/navigate! (spies/create)]
      (let [hide-spy (spies/create)
            result (((shared.interactions/delete-sim ::id) hide-spy) ::event)]
        (testing "deletes the simulator"
          (is (spies/called-with? actions/delete-simulator ::id))
          (is (spies/called-with? store/dispatch ::action))
          (is (spies/called-with? ch/then ::request (spies/matcher fn?)))
          (is (spies/called-with? ch/catch ::request (spies/matcher fn?)))
          (is (spies/called-with? ch/finally ::request (spies/matcher fn?)))
          (is (= result ::handled)))

        (testing "handles a success"
          (let [[_ on-success] (first (spies/calls ch/then))]
            (spies/reset! hide-spy nav/navigate! shared.interactions/toast)
            (on-success ::body)
            (is (spies/called-with? shared.interactions/toast ::body :success (spies/matcher string?)))
            (is (spies/never-called? hide-spy))
            (is (spies/called-with? nav/navigate! :home))))

        (testing "handles an error"
          (let [[_ on-error] (first (spies/calls ch/catch))]
            (spies/reset! hide-spy nav/navigate! shared.interactions/toast)
            (on-error ::body)
            (is (spies/called-with? shared.interactions/toast ::body :error (spies/matcher string?)))
            (is (spies/never-called? hide-spy))
            (is (spies/never-called? nav/navigate!))))

        (testing "hides the modal"
          (let [[_ on-finally] (first (spies/calls ch/finally))]
            (spies/reset! hide-spy)
            (on-finally ::result)
            (is (spies/called? hide-spy))))))))

(deftest ^:unit reset-config-test
  (testing "(reset-config)"
    (with-redefs [shared.interactions/toast (spies/create)
                  actions/reset-simulator-config (spies/constantly ::action)
                  store/dispatch (spies/constantly ::request)
                  ch/then (spies/create (fn [ch _] ch))
                  ch/catch (spies/constantly ::handled)]
      (let [reset-spy (spies/create)
            sim->model-spy (spies/constantly ::new-model)
            form (reify
                   IReset
                   (-reset! [_ model]
                     (reset-spy model)))
            result ((shared.interactions/reset-config form sim->model-spy ::id ::type) ::event)]
        (testing "resets the config"
          (is (spies/called-with? actions/reset-simulator-config ::id ::type))
          (is (spies/called-with? store/dispatch ::action))
          (is (spies/called-with? ch/then ::request (spies/matcher fn?)))
          (is (spies/called-with? ch/catch ::request (spies/matcher fn?)))
          (is (= result ::handled)))

        (testing "handles a success"
          (let [[_ on-success] (first (spies/calls ch/then))]
            (spies/reset! reset-spy sim->model-spy shared.interactions/toast)
            (on-success {:simulator ::simulator})
            (is (spies/called-with? sim->model-spy ::simulator))
            (is (spies/called-with? reset-spy ::new-model))
            (is (spies/called-with? shared.interactions/toast {:simulator ::simulator} :success (spies/matcher string?)))))

        (testing "handles an error"
          (let [[_ on-error] (first (spies/calls ch/catch))]
            (spies/reset! reset-spy sim->model-spy shared.interactions/toast)
            (on-error ::body)
            (is (spies/never-called? sim->model-spy))
            (is (spies/never-called? reset-spy))
            (is (spies/called-with? shared.interactions/toast ::body :error (spies/matcher string?)))))))))

(deftest ^:unit create-simulator-test
  (testing "(create-simulator)"
    (testing "when the form is creatable"
      (with-redefs [shared.interactions/creatable? (constantly true)
                    shared.interactions/toast (spies/create)
                    actions/create-simulator (spies/constantly ::action)
                    store/dispatch (spies/constantly ::request)
                    ch/then (spies/create (fn [ch _] ch))
                    ch/catch (spies/constantly ::handled)
                    nav/nav-and-replace! (spies/create)]
        (let [reset-spy (spies/create)
              form (reify
                     IDeref
                     (-deref [_]
                       ::model)
                     IReset
                     (-reset! [_ value]
                       (reset-spy value)))
              result ((shared.interactions/create-simulator form {::model ::source}) ::event)]
          (testing "updates the simulator"
            (is (spies/called-with? actions/create-simulator ::source))
            (is (spies/called-with? store/dispatch ::action))
            (is (spies/called-with? ch/then ::request (spies/matcher fn?)))
            (is (spies/called-with? ch/catch ::request (spies/matcher fn?)))
            (is (= result ::handled)))

          (testing "handles a success"
            (let [[_ on-success] (first (spies/calls ch/then))
                  body {:simulator {:id ::id ::with ::more}}]
              (spies/reset! reset-spy nav/nav-and-replace! shared.interactions/toast)
              (on-success body)
              (is (spies/called-with? reset-spy ::model))
              (is (spies/called-with? nav/nav-and-replace! :details {:id ::id}))
              (is (spies/called-with? shared.interactions/toast body :success (spies/matcher string?)))))

          (testing "handles an error"
            (let [[_ on-error] (first (spies/calls ch/catch))]
              (spies/reset! reset-spy nav/nav-and-replace! shared.interactions/toast)
              (on-error ::body)
              (is (spies/never-called? reset-spy))
              (is (spies/never-called? nav/nav-and-replace!))
              (is (spies/called-with? shared.interactions/toast ::body :error (spies/matcher string?))))))))

    (testing "when the form is not creatable"
      (with-redefs [shared.interactions/creatable? (constantly false)
                    ch/reject (constantly ::rejected)]
        (let [form (reify
                     IDeref
                     (-deref [_]))]
          (is (= ::rejected ((shared.interactions/create-simulator form ::model->source) ::event))))))))

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
