(ns com.ben-allred.clj-app-simulator.ui.simulators.http.views-test
  (:require [cljs.test :as t :refer [deftest testing is]]
            [test.utils.spies :as spies]
            [test.utils.dom :as test.dom]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.views :as http.views]
            [com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]
            [com.ben-allred.clj-app-simulator.ui.services.forms.fields :as fields]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.resources :as resources]
            [com.ben-allred.clj-app-simulator.ui.utils.dom :as dom]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.utils.moment :as mo]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.modals :as modals]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.views :as shared.views]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as shared.interactions]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.transformations :as tr]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.interactions :as interactions]
            [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]))

(deftest ^:unit name-field-test
  (testing "(name-field)"
    (is (= [shared.views/name-field ::form tr/model->view tr/view->model]
           (http.views/name-field ::form)))))

(deftest ^:unit group-field-test
  (testing "(group-field)"
    (is (= [shared.views/group-field ::form tr/model->view tr/view->model]
           (http.views/group-field ::form)))))

(deftest ^:unit description-field-test
  (testing "(description-field)"
    (is (= [shared.views/description-field ::form tr/model->view tr/view->model]
           (http.views/description-field ::form)))))

(deftest ^:unit status-field-test
  (testing "(status-field)"
    (let [with-attrs-spy (spies/create (fn [v & _] (assoc v :more ::attrs)))]
      (with-redefs [shared.views/with-attrs with-attrs-spy]
        (testing "renders the form field"
          (let [[node attrs resource] (http.views/status-field ::form)]
            (is (spies/called-with? with-attrs-spy
                                    (spies/matcher any?)
                                    ::form
                                    [:response :status]
                                    tr/model->view
                                    tr/view->model))
            (is (= fields/select node))
            (is (= "Status" (:label attrs)))
            (is (= ::attrs (:more attrs)))
            (is (= resources/statuses resource))))))))

(deftest ^:unit delay-field-test
  (testing "(delay-field)"
    (let [with-attrs-spy (spies/create (fn [v & _] (assoc v :more ::attrs)))]
      (with-redefs [shared.views/with-attrs with-attrs-spy]
        (testing "renders the form field"
          (let [[node attrs] (http.views/delay-field ::form)]
            (is (spies/called-with? with-attrs-spy
                                    (spies/matcher any?)
                                    ::form
                                    [:delay]
                                    tr/model->view
                                    tr/view->model))
            (is (= fields/input node))
            (is (= "Delay (ms)" (:label attrs)))
            (is (= ::attrs (:more attrs)))))))))

(deftest ^:unit headers-field-test
  (testing "(headers-field)"
    (let [update-spy (spies/create)
          current-spy (spies/create (constantly {:response {:headers ::headers}}))
          error-spy (spies/create (constantly {:response {:headers ::errors}}))]
      (with-redefs [forms/update-in update-spy
                    forms/current-model current-spy
                    forms/errors error-spy]
        (let [root (http.views/headers-field ::form)
              multi (test.dom/query-one root fields/multi)
              attrs (test.dom/attrs multi)]
          (testing "renders multi field"
            (is (= fields/header (last multi))))

          (testing "has a :label"
            (is (= "Headers" (:label attrs))))

          (testing "has a :key-fn function which produces a key"
            (let [key-fn (:key-fn attrs)]
              (is (= "header-key" (key-fn ["key" ::whatever])))))

          (testing "has a :new-fn function"
            (let [new-fn (:new-fn attrs)]
              (is (= ["" ""] (new-fn ::anything-at-all)))))

          (testing "has a :change-fn function which updates the form"
            (let [change-fn (:change-fn attrs)]
              (change-fn :a :b :c)
              (is (spies/called-with? update-spy ::form [:response :headers] :a :b :c))))

          (testing "has a :value"
            (is (= ::headers (:value attrs))))

          (testing "has a :to-view function"
            (let [to-view (:to-view attrs)]
              (is (= ["Some-Header" "some-value"] (to-view [:some-header "some-value"])))))

          (testing "has a :to-model function"
            (let [to-model (:to-model attrs)]
              (is (= [:some-header "some-value"] (to-model ["Some-Header" "some-value"])))))

          (testing "has :errors"
            (is (= ::errors (:errors attrs)))))))))

(deftest ^:unit body-field-test
  (testing "(body-field)"
    (let [with-attrs-spy (spies/create (fn [v & _] (assoc v :more ::attrs)))]
      (with-redefs [shared.views/with-attrs with-attrs-spy]
        (testing "renders the form field"
          (let [[node attrs] (http.views/body-field ::form)]
            (is (spies/called-with? with-attrs-spy
                                    (spies/matcher any?)
                                    ::form
                                    [:response :body]
                                    tr/model->view
                                    tr/view->model))
            (is (= fields/textarea node))
            (is (= "Body" (:label attrs)))
            (is (= ::attrs (:more attrs)))))))))

(deftest ^:unit method-field-test
  (testing "(method-field)"
    (let [with-attrs-spy (spies/create (fn [v & _] (assoc v :more ::attrs)))]
      (with-redefs [shared.views/with-attrs with-attrs-spy]
        (testing "renders the form field"
          (let [[node attrs resource] (http.views/method-field ::form)]
            (is (spies/called-with? with-attrs-spy
                                    (spies/matcher any?)
                                    ::form
                                    [:method]
                                    tr/model->view
                                    tr/view->model))
            (is (= fields/select node))
            (is (= "HTTP Method" (:label attrs)))
            (is (= ::attrs (:more attrs)))
            (is (= resources/http-methods resource))))))))

(deftest ^:unit path-field-test
  (testing "(path-field)"
    (let [with-attrs-spy (spies/create (fn [v & _] (assoc v :more ::attrs)))]
      (with-redefs [shared.views/with-attrs with-attrs-spy]
        (testing "renders the form field"
          (let [[node attrs] (http.views/path-field ::form)]
            (is (spies/called-with? with-attrs-spy
                                    (spies/matcher any?)
                                    ::form
                                    [:path]
                                    tr/model->view
                                    tr/view->model))
            (is (= fields/input node))
            (is (= "Path" (:label attrs)))
            (is (= ::attrs (:more attrs)))))))))

(deftest ^:unit sim-edit-form*-test
  (testing "(sim-edit-form*)"
    (let [model {:response {:status ::status
                            :body   ""}
                 :name     nil}
          errors-spy (spies/create)
          changed-spy (spies/create)
          model-spy (spies/create (constantly model))
          event-spy (spies/create)
          action-spy (spies/create (constantly ::action))
          dispatch-spy (spies/create)]
      (with-redefs [forms/errors errors-spy
                    forms/changed? changed-spy
                    forms/current-model model-spy
                    dom/prevent-default event-spy
                    actions/update-simulator action-spy
                    store/dispatch dispatch-spy]
        (testing "when rendering a form"
          (let [root (http.views/sim-edit-form* ::id ::form)
                edit-form (test.dom/query-one root :.simulator-edit)]
            (testing "renders a name field"
              (let [node (test.dom/query-one edit-form http.views/name-field)]
                (is (= [http.views/name-field ::form] node))))

            (testing "renders a group field"
              (let [node (test.dom/query-one edit-form http.views/group-field)]
                (is (= [http.views/group-field ::form] node))))

            (testing "renders a description field"
              (let [node (test.dom/query-one edit-form http.views/description-field)]
                (is (= [http.views/description-field ::form] node))))

            (testing "renders a status field"
              (let [node (test.dom/query-one edit-form http.views/status-field)]
                (is (= [http.views/status-field ::form] node))))

            (testing "renders a delay field"
              (let [node (test.dom/query-one edit-form http.views/delay-field)]
                (is (= [http.views/delay-field ::form] node))))

            (testing "renders a headers field"
              (let [node (test.dom/query-one edit-form http.views/headers-field)]
                (is (= [http.views/headers-field ::form] node))))

            (testing "renders a body field"
              (let [node (test.dom/query-one edit-form http.views/body-field)]
                (is (= [http.views/body-field ::form] node))))

            (testing "when resetting the simulator"
              (spies/reset! dispatch-spy action-spy)
              (testing "dispatches an action"))))))))

(deftest ^:unit sim-edit-form-test
  (testing "(sim-edit-form)"
    (let [form-spy (spies/create (constantly ::form))
          simulator {:config {:useless     ::thing
                              :also        ::useless
                              :group       ::group
                              :name        ::name
                              :description ::description
                              :response    {:status  ::status
                                            :body    ::body
                                            :headers {:header-c ["header-c"]
                                                      :header-a "thing"
                                                      :header-b ["double" "things"]}}}
                     :id     ::id}]
      (with-redefs [forms/create form-spy]
        (let [root (http.views/sim-edit-form simulator)
              expected {:group       ::group
                        :name        ::name
                        :description ::description
                        :delay       0
                        :response    {:status  ::status
                                      :body    ::body
                                      :headers [[:header-a "thing"]
                                                [:header-b "double"]
                                                [:header-b "things"]
                                                [:header-c "header-c"]]}}]
          (testing "creates a form from source data"
            (is (spies/called-with? form-spy expected resources/validate-existing)))

          (testing "returns a function that renders the form"
            (is (= [http.views/sim-edit-form* ::id ::form]
                   (root simulator)))))))))

(deftest ^:unit sim-request-test
  (testing "(sim-request)"
    (let [moment-spy (spies/create (constantly ::moment))
          from-now-spy (spies/create (constantly ::from-now))
          action-spy (spies/create (constantly ::action))
          dispatch-spy (spies/create)]
      (with-redefs [mo/->moment moment-spy
                    mo/from-now from-now-spy
                    actions/show-modal action-spy
                    store/dispatch dispatch-spy]
        (let [request {:timestamp ::timestamp :details ::details}
              root (http.views/sim-request ::sim request)
              tree (test.dom/query-one root :.request)]
          (testing "converts timestamp to moment"
            (is (spies/called-with? moment-spy ::timestamp)))

          (testing "when clicking the tree"
            (spies/reset! action-spy dispatch-spy)
            (test.dom/simulate-event tree :click)
            (testing "shows the modal"
              (is (spies/called-with? action-spy
                                      [modals/request-modal ::sim (assoc request :dt ::moment)]
                                      "Request Details"))
              (is (spies/called-with? dispatch-spy ::action))))

          (testing "displays moment from now"
            (is (test.dom/contains? tree ::from-now))))))))

(deftest ^:unit sim-test
  (testing "(sim)"
    (let [sim {:id       ::simulator-id
               :config   {::some ::simulator}
               :requests [{:timestamp 123
                           ::data     ::123}
                          {:timestamp 456
                           ::data     ::456}]}
          clear-spy (spies/create (constantly ::clear))
          delete-spy (spies/create (constantly ::delete))]
      (with-redefs [shared.interactions/clear-requests clear-spy
                    shared.interactions/show-delete-modal delete-spy]
        (let [root (http.views/sim sim)]
          (testing "displays sim-details"
            (let [details (test.dom/query-one root shared.views/sim-details)]
              (is (= [shared.views/sim-details sim] details))))

          (testing "displays sim-edit-form"
            (let [form (test.dom/query-one root http.views/sim-edit-form)]
              (is (= [http.views/sim-edit-form sim] form))))

          (testing "displays a list of sim-request components"
            (let [[req-1 req-2 :as sim-reqs] (test.dom/query-all root http.views/sim-request)]
              (is (= 2 (count sim-reqs)))
              (is (= "456" (:key (meta req-1))))
              (is (= [http.views/sim-request {::some ::simulator} {:timestamp 456 ::data ::456}]
                     req-1))
              (is (= "123" (:key (meta req-2))))
              (is (= [http.views/sim-request {::some ::simulator} {:timestamp 123 ::data ::123}]
                     req-2))))

          (testing "has a button to clear requests"
            (let [button (test.dom/query-one root :.button.clear-button)]
              (is (= ::clear (:on-click (test.dom/attrs button))))
              (is (spies/called-with? clear-spy ::simulator-id))

              (testing "when there are no requests"
                (testing "is not disabled"
                  (is (not (:disabled (test.dom/attrs button)))))))

            (testing "when there are no requests"
              (let [root (http.views/sim {})
                    button (test.dom/query-one root :.button.clear-button)]
                (testing "is disabled"
                  (is (:disabled (test.dom/attrs button)))))))

          (testing "has a button to delete the simulators"
            (let [button (test.dom/query-one root :.button.delete-button)]
              (is (= ::delete (:on-click (test.dom/attrs button))))
              (is (spies/called-with? delete-spy ::simulator-id)))))))))

(deftest ^:unit sim-create-form*-test
  (testing "(sim-create-form*)"
    (let [errors-spy (spies/create)
          create-spy (spies/create (constantly ::submit))
          nav-spy (spies/create (constantly ::href))]
      (with-redefs [forms/errors errors-spy
                    interactions/create-simulator create-spy
                    nav/path-for nav-spy]
        (let [root (http.views/sim-create-form* ::form)
              form (test.dom/query-one root :.simulator-create)]
          (testing "handles submit"
            (is (spies/called-with? create-spy ::form true))
            (is (-> form
                    (test.dom/attrs)
                    (:on-submit)
                    (= ::submit))))

          (testing "renders the form components"
            (is (test.dom/contains? form [http.views/method-field ::form]))
            (is (test.dom/contains? form [http.views/path-field ::form]))
            (is (test.dom/contains? form [http.views/name-field ::form]))
            (is (test.dom/contains? form [http.views/group-field ::form]))
            (is (test.dom/contains? form [http.views/description-field ::form]))
            (is (test.dom/contains? form [http.views/status-field ::form]))
            (is (test.dom/contains? form [http.views/delay-field ::form]))
            (is (test.dom/contains? form [http.views/headers-field ::form]))
            (is (test.dom/contains? form [http.views/body-field ::form])))

          (testing "renders a reset button"
            (is (spies/called-with? nav-spy :home))
            (is (-> form
                    (test.dom/query-one :.reset-button)
                    (test.dom/attrs)
                    (:href)
                    (= ::href))))

          (testing "renders a save button"
            (let [button (test.dom/query-one form :.save-button)]
              (is button)
              (is (not (:disabled (test.dom/attrs button)))))))

        (testing "when there are errors"
          (spies/reset! errors-spy create-spy)
          (spies/respond-with! errors-spy (constantly ::errors))
          (let [root (http.views/sim-create-form* ::form)]
            (is (spies/called-with? errors-spy ::form))
            (is (spies/called-with? create-spy ::form false))
            (is (-> root
                    (test.dom/query-one :.save-button)
                    (test.dom/attrs)
                    (:disabled)))))))))

(deftest ^:unit sim-create-form-test
  (testing "(sim-create-form)"
    (let [form-spy (spies/create (constantly ::form))]
      (with-redefs [forms/create form-spy]
        (testing "renders the form"
          (let [component (http.views/sim-create-form)]
            (is (spies/called-with? form-spy
                                    {:response {:status 200}
                                     :method   :http/get
                                     :path     "/"
                                     :delay    0}
                                    resources/validate-new))
            (let [root (component)]
              (is (test.dom/contains? root [http.views/sim-create-form* ::form])))))))))

(defn run-tests [] (t/run-tests))
