(ns com.ben-allred.clj-app-simulator.ui.simulators.file.views-test
  (:require [cljs.test :as t :refer [deftest testing is]]
            [test.utils.spies :as spies]
            [test.utils.dom :as test.dom]
            [com.ben-allred.clj-app-simulator.ui.simulators.file.views :as file.views]
            [com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]
            [com.ben-allred.clj-app-simulator.ui.services.forms.fields :as fields]
            [com.ben-allred.clj-app-simulator.ui.simulators.file.resources :as resources]
            [com.ben-allred.clj-app-simulator.ui.utils.dom :as dom]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.views :as shared.views]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as shared.interactions]
            [com.ben-allred.clj-app-simulator.ui.simulators.file.transformations :as tr]
            [com.ben-allred.clj-app-simulator.ui.simulators.file.interactions :as interactions]
            [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]))

(deftest ^:unit path-field-test
  (testing "(path-field)"
    (is (= [shared.views/path-field ::form tr/model->view tr/view->model]
           (file.views/path-field ::form)))))

(deftest ^:unit name-field-test
  (testing "(name-field)"
    (is (= [shared.views/name-field ::form tr/model->view tr/view->model]
           (file.views/name-field ::form)))))

(deftest ^:unit group-field-test
  (testing "(group-field)"
    (is (= [shared.views/group-field ::form tr/model->view tr/view->model]
           (file.views/group-field ::form)))))

(deftest ^:unit description-field-test
  (testing "(description-field)"
    (is (= [shared.views/description-field ::form tr/model->view tr/view->model]
           (file.views/description-field ::form)))))

(deftest ^:unit status-field-test
  (testing "(status-field)"
    (let [with-attrs-spy (spies/create (fn [v & _] (assoc v :more ::attrs)))]
      (with-redefs [shared.views/with-attrs with-attrs-spy]
        (testing "renders the form field"
          (let [[node attrs resource] (file.views/status-field ::form)]
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
          (let [[node attrs] (file.views/delay-field ::form)]
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
          current-spy (spies/constantly {:response {:headers ::headers}})
          error-spy (spies/constantly {:response {:headers ::errors}})]
      (with-redefs [forms/update-in update-spy
                    forms/current-model current-spy
                    forms/errors error-spy]
        (let [root (file.views/headers-field ::form)
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

(deftest ^:unit file-field-test
  (testing "(file-field)"


    ))

(deftest ^:unit method-field-test
  (testing "(method-field)"
    (let [with-attrs-spy (spies/create (fn [v & _] (assoc v :more ::attrs)))]
      (with-redefs [shared.views/with-attrs with-attrs-spy]
        (testing "renders the form field"
          (let [[node attrs resource] (file.views/method-field ::form)]
            (is (spies/called-with? with-attrs-spy
                                    (spies/matcher any?)
                                    ::form
                                    [:method]
                                    tr/model->view
                                    tr/view->model))
            (is (= fields/select node))
            (is (= "HTTP Method" (:label attrs)))
            (is (= ::attrs (:more attrs)))
            (is (= resources/file-methods resource))))))))

(deftest ^:unit sim-edit-form*-test
  (testing "(sim-edit-form*)"
    (let [model {:response {:status ::status
                            :body   ""}
                 :name     nil}
          errors-spy (spies/create)
          changed-spy (spies/create)
          model-spy (spies/constantly model)
          event-spy (spies/create)
          action-spy (spies/constantly ::action)
          dispatch-spy (spies/create)]
      (with-redefs [forms/errors errors-spy
                    forms/changed? changed-spy
                    forms/current-model model-spy
                    dom/prevent-default event-spy
                    actions/update-simulator action-spy
                    store/dispatch dispatch-spy]
        (testing "when rendering a form"
          (let [root (file.views/sim-edit-form* ::id ::form ::uploads)
                edit-form (test.dom/query-one root :.simulator-edit)]
            (testing "renders a name field"
              (let [node (test.dom/query-one edit-form file.views/name-field)]
                (is (= [file.views/name-field ::form] node))))

            (testing "renders a group field"
              (let [node (test.dom/query-one edit-form file.views/group-field)]
                (is (= [file.views/group-field ::form] node))))

            (testing "renders a description field"
              (let [node (test.dom/query-one edit-form file.views/description-field)]
                (is (= [file.views/description-field ::form] node))))

            (testing "renders a status field"
              (let [node (test.dom/query-one edit-form file.views/status-field)]
                (is (= [file.views/status-field ::form] node))))

            (testing "renders a delay field"
              (let [node (test.dom/query-one edit-form file.views/delay-field)]
                (is (= [file.views/delay-field ::form] node))))

            (testing "renders a headers field"
              (let [node (test.dom/query-one edit-form file.views/headers-field)]
                (is (= [file.views/headers-field ::form] node))))

            (testing "renders a file field"
              (let [node (test.dom/query-one edit-form file.views/file-field)]
                (is (= [file.views/file-field ::form ::uploads] node))))

            (testing "when resetting the simulator"
              (spies/reset! dispatch-spy action-spy)
              (testing "dispatches an action"))))))))

(deftest ^:unit sim-edit-form-test
  (testing "(sim-edit-form)"
    (let [form-spy (spies/constantly ::form)
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
        (let [root (file.views/sim-edit-form simulator ::uploads)
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
            (is (= [file.views/sim-edit-form* ::id ::form ::uploads]
                   (root simulator ::uploads)))))))))

(deftest ^:unit sim-test
  (testing "(sim)"
    (let [sim {:id       ::simulator-id
               :config   {::some ::simulator}
               :requests [{:timestamp 123
                           ::data     ::123}
                          {:timestamp 456
                           ::data     ::456}]}
          clear-spy (spies/constantly ::clear)
          delete-spy (spies/constantly ::delete)]
      (with-redefs [shared.interactions/clear-requests clear-spy
                    shared.interactions/show-delete-modal delete-spy]
        (let [root (file.views/sim sim ::uploads)]
          (testing "displays sim-details"
            (let [details (test.dom/query-one root shared.views/sim-details)]
              (is (= [shared.views/sim-details sim] details))))

          (testing "displays sim-edit-form"
            (let [form (test.dom/query-one root file.views/sim-edit-form)]
              (is (= [file.views/sim-edit-form sim ::uploads] form))))

          (testing "displays a list of sim-request components"
            (let [[req-1 req-2 :as sim-reqs] (test.dom/query-all root shared.views/sim-request)]
              (is (= 2 (count sim-reqs)))
              (is (= "456" (:key (meta req-1))))
              (is (= [shared.views/sim-request {::some ::simulator} {:timestamp 456 ::data ::456}]
                     req-1))
              (is (= "123" (:key (meta req-2))))
              (is (= [shared.views/sim-request {::some ::simulator} {:timestamp 123 ::data ::123}]
                     req-2))))

          (testing "has a button to clear requests"
            (let [button (test.dom/query-one root :.button.clear-button)]
              (is (= ::clear (:on-click (test.dom/attrs button))))
              (is (spies/called-with? clear-spy ::simulator-id))

              (testing "when there are no requests"
                (testing "is not disabled"
                  (is (not (:disabled (test.dom/attrs button)))))))

            (testing "when there are no requests"
              (let [root (file.views/sim {} ::uploads)
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
          create-spy (spies/constantly ::submit)
          nav-spy (spies/constantly ::href)]
      (with-redefs [forms/errors errors-spy
                    interactions/create-simulator create-spy
                    nav/path-for nav-spy]
        (let [root (file.views/sim-create-form* ::form ::uploads)
              form (test.dom/query-one root :.simulator-create)]
          (testing "handles submit"
            (is (spies/called-with? create-spy ::form true))
            (is (-> form
                    (test.dom/attrs)
                    (:on-submit)
                    (= ::submit))))

          (testing "renders the form components"
            (is (test.dom/contains? form [file.views/method-field ::form]))
            (is (test.dom/contains? form [file.views/path-field ::form]))
            (is (test.dom/contains? form [file.views/name-field ::form]))
            (is (test.dom/contains? form [file.views/group-field ::form]))
            (is (test.dom/contains? form [file.views/description-field ::form]))
            (is (test.dom/contains? form [file.views/status-field ::form]))
            (is (test.dom/contains? form [file.views/delay-field ::form]))
            (is (test.dom/contains? form [file.views/headers-field ::form]))
            (is (test.dom/contains? form [file.views/file-field ::form ::uploads])))

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
          (let [root (file.views/sim-create-form* ::form ::uploads)]
            (is (spies/called-with? errors-spy ::form))
            (is (spies/called-with? create-spy ::form false))
            (is (-> root
                    (test.dom/query-one :.save-button)
                    (test.dom/attrs)
                    (:disabled)))))))))

(deftest ^:unit sim-create-form-test
  (testing "(sim-create-form)"
    (let [form-spy (spies/constantly ::form)]
      (with-redefs [forms/create form-spy]
        (testing "renders the form"
          (let [component (file.views/sim-create-form [{:id ::file}])]
            (is (spies/called-with? form-spy
                                    {:response {:status 200
                                                :file ::file}
                                     :method   :file/get
                                     :path     "/"
                                     :delay    0}
                                    resources/validate-new))
            (let [root (component ::uploads)]
              (is (test.dom/contains? root [file.views/sim-create-form* ::form ::uploads])))))))))

(defn run-tests []
  (t/run-tests))
