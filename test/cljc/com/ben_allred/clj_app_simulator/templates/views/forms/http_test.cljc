(ns com.ben-allred.clj-app-simulator.templates.views.forms.http-test
  (:require
    #?@(:cljs
        [[com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]
         [com.ben-allred.clj-app-simulator.ui.simulators.http.interactions :as interactions]
         [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as shared.interactions]])
    [clojure.test :as t :refer [deftest is testing]]
    [com.ben-allred.clj-app-simulator.services.navigation :as nav*]
    [com.ben-allred.clj-app-simulator.templates.fields :as fields]
    [com.ben-allred.clj-app-simulator.templates.resources.http :as resources]
    [com.ben-allred.clj-app-simulator.templates.transformations.http :as tr]
    [com.ben-allred.clj-app-simulator.templates.views.forms.http :as http.views]
    [com.ben-allred.clj-app-simulator.templates.views.forms.shared :as shared.views]
    [com.ben-allred.clj-app-simulator.templates.views.simulators :as views.sim]
    [test.utils.dom :as test.dom]
    [test.utils.spies :as spies]))

(deftest ^:unit path-field-test
  (testing "(path-field)"
    (is (= [shared.views/path-field ::form tr/model->view tr/view->model]
           (http.views/path-field ::form)))))

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
          current-spy (spies/constantly {:response {:headers ::headers}})
          error-spy (spies/constantly {:response {:headers ::errors}})]
      (with-redefs [#?@(:cljs [forms/update-in update-spy
                               forms/current-model current-spy
                               forms/display-errors error-spy])]
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

          #?(:cljs
             (testing "has a :change-fn function which updates the form"
               (let [change-fn (:change-fn attrs)]
                 (change-fn :a :b :c)
                 (is (spies/called-with? update-spy ::form [:response :headers] :a :b :c)))))

          #?(:cljs
             (testing "has a :value"
               (is (= ::headers (:value attrs)))))

          #?(:cljs
             (testing "has a :to-view function"
               (let [to-view (:to-view attrs)]
                 (is (= ["Some-Header" "some-value"] (to-view [:some-header "some-value"]))))))

          #?(:cljs
             (testing "has a :to-model function"
               (let [to-model (:to-model attrs)]
                 (is (= [:some-header "some-value"] (to-model ["Some-Header" "some-value"]))))))

          #?(:cljs
             (testing "has :errors"
               (is (= ::errors (:errors attrs))))))))))

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

(deftest ^:unit sim-edit-form*-test
  (testing "(sim-edit-form*)"
    (let [errors-spy (spies/create)
          changed-spy (spies/constantly true)
          update-spy (spies/constantly ::submit)
          reset-spy (spies/constantly ::reset)]
      (with-redefs [#?@(:cljs [forms/display-errors errors-spy
                               forms/changed? changed-spy
                               interactions/update-simulator update-spy
                               interactions/reset-simulator reset-spy])]
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

            #?(:cljs
               (testing "and when resetting the simulator"
                 (testing "dispatches an action"
                   (is (-> edit-form
                           (test.dom/query-one :.reset-button)
                           (test.dom/attrs)
                           (:on-click)
                           (= ::reset)))
                   (is (spies/called-with? reset-spy ::form ::id))))))

          #?(:cljs
             (testing "and when submitting the form"
               (testing "and when there are errors"
                 (spies/reset! update-spy errors-spy changed-spy)
                 (spies/respond-with! errors-spy (constantly ::errors))
                 (let [root (http.views/sim-edit-form* ::id ::form)
                       edit-form (test.dom/query-one root :.simulator-edit)]
                   (testing "has an :on-submit attr"
                     (is (-> edit-form
                             (test.dom/query-one :.simulator-edit)
                             (test.dom/attrs)
                             (:on-submit)
                             (= ::submit))))

                   (testing "has a disabled save button"
                     (is (-> edit-form
                             (test.dom/query-one :.save-button)
                             (test.dom/attrs)
                             (:disabled))))))

               (testing "and when there are no changes"
                 (spies/reset! update-spy errors-spy changed-spy)
                 (spies/respond-with! changed-spy (constantly false))
                 (let [root (http.views/sim-edit-form* ::id ::form)
                       edit-form (test.dom/query-one root :.simulator-edit)]
                   (testing "has an :on-submit attr"
                     (is (-> edit-form
                             (test.dom/query-one :.simulator-edit)
                             (test.dom/attrs)
                             (:on-submit)
                             (= ::submit))))

                   (testing "has a disabled save button"
                     (is (-> edit-form
                             (test.dom/query-one :.save-button)
                             (test.dom/attrs)
                             (:disabled))))))

               (testing "and when there are no errors and changes"
                 (spies/reset! update-spy errors-spy changed-spy)
                 (let [root (http.views/sim-edit-form* ::id ::form)
                       edit-form (test.dom/query-one root :.simulator-edit)]
                   (testing "has an :on-submit attr"
                     (is (-> edit-form
                             (test.dom/query-one :.simulator-edit)
                             (test.dom/attrs)
                             (:on-submit)
                             (= ::submit))))

                   (testing "has an enabled :on-submit"
                     (is (spies/called-with? update-spy ::form ::id)))

                   (testing "has an enabled save button"
                     (is (-> edit-form
                             (test.dom/query-one :.save-button)
                             (test.dom/attrs)
                             (:disabled)
                             (not)))))))))))))

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
      (with-redefs [#?@(:cljs [forms/create form-spy])]
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
          #?(:cljs
             (testing "creates a form from source data"
               (is (spies/called-with? form-spy expected resources/validate-existing))))

          (testing "returns a function that renders the form"
            (is (= [http.views/sim-edit-form* ::id #?(:clj expected :cljs ::form)]
                   (root simulator)))))))))

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
      (with-redefs [#?@(:cljs [shared.interactions/clear-requests clear-spy
                               shared.interactions/show-delete-modal delete-spy])]
        (let [root (http.views/sim sim)]
          (testing "displays sim-details"
            (let [details (test.dom/query-one root views.sim/sim-details)]
              (is (= [views.sim/sim-details sim] details))))

          (testing "displays sim-edit-form"
            (let [form (test.dom/query-one root http.views/sim-edit-form)]
              (is (= [http.views/sim-edit-form sim] form))))

          (testing "displays a list of sim-request components"
            (let [[req-1 req-2 :as sim-reqs] (test.dom/query-all root shared.views/sim-request)]
              (is (= 2 (count sim-reqs)))
              (is (= "456" (:key (meta req-1))))
              (is (= [shared.views/sim-request {::some ::simulator} {:timestamp 456 ::data ::456}]
                     req-1))
              (is (= "123" (:key (meta req-2))))
              (is (= [shared.views/sim-request {::some ::simulator} {:timestamp 123 ::data ::123}]
                     req-2))))

          #?(:cljs
             (testing "has a button to clear requests"
               (let [button (test.dom/query-one root :.button.clear-button)]
                 (is (= ::clear (:on-click (test.dom/attrs button))))
                 (is (spies/called-with? clear-spy :http ::simulator-id))

                 (testing "when there are no requests"
                   (testing "is not disabled"
                     (is (not (:disabled (test.dom/attrs button)))))))

               (testing "when there are no requests"
                 (let [root (http.views/sim {})
                       button (test.dom/query-one root :.button.clear-button)]
                   (testing "is disabled"
                     (is (:disabled (test.dom/attrs button))))))))

          #?(:cljs
             (testing "has a button to delete the simulators"
               (let [button (test.dom/query-one root :.button.delete-button)]
                 (is (= ::delete (:on-click (test.dom/attrs button))))
                 (is (spies/called-with? delete-spy ::simulator-id))))))))))

(deftest ^:unit sim-create-form*-test
  (testing "(sim-create-form*)"
    (let [errors-spy (spies/create)
          create-spy (spies/constantly ::submit)
          nav-spy (spies/constantly ::href)]
      (with-redefs [nav*/path-for nav-spy
                    #?@(:cljs [forms/display-errors errors-spy
                               interactions/create-simulator create-spy])]
        (let [root (http.views/sim-create-form* ::form)
              form (test.dom/query-one root :.simulator-create)]
          #?(:cljs
             (testing "handles submit"
               (is (spies/called-with? create-spy ::form))
               (is (-> form
                       (test.dom/attrs)
                       (:on-submit)
                       (= ::submit)))))

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
            (let [button (test.dom/query-one form :.save-button)
                  attrs (test.dom/attrs button)]
              (is button)
              (is #?(:clj  (:disabled attrs)
                     :cljs (not (:disabled attrs)))))))

        #?(:cljs
           (testing "when there are errors"
             (spies/reset! errors-spy create-spy)
             (spies/respond-with! errors-spy (constantly ::errors))
             (testing "renders a disabled save button"
               (let [root (http.views/sim-create-form* ::form)]
                 (is (spies/called-with? errors-spy ::form))
                 (is (spies/called-with? create-spy ::form))
                 (is (-> root
                         (test.dom/query-one :.save-button)
                         (test.dom/attrs)
                         (:disabled)))))))))))

(deftest ^:unit sim-create-form-test
  (testing "(sim-create-form)"
    (let [form-spy (spies/constantly ::form)]
      (with-redefs [#?@(:cljs [forms/create form-spy])]
        (testing "renders the form"
          (let [component (http.views/sim-create-form)
                model {:response {:status 200
                                  :body   nil}
                       :method   :http/get
                       :path     "/"
                       :delay    0}]
            #?(:cljs
               (is (spies/called-with? form-spy model resources/validate-new)))
            (let [root (component)]
              (is (test.dom/contains? root [http.views/sim-create-form* #?(:clj model :cljs ::form)])))))))))

(defn run-tests []
  (t/run-tests))
