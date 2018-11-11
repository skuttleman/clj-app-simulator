(ns com.ben-allred.app-simulator.templates.views.forms.http-test
  (:require
    #?@(:cljs
        [[com.ben-allred.app-simulator.ui.services.forms.core :as forms]
         [com.ben-allred.app-simulator.ui.services.forms.standard :as form]
         [com.ben-allred.app-simulator.ui.simulators.http.interactions :as interactions]
         [com.ben-allred.app-simulator.ui.simulators.shared.interactions :as shared.interactions]])
    [clojure.test :as t :refer [deftest is testing]]
    [com.ben-allred.app-simulator.services.navigation :as nav*]
    [com.ben-allred.app-simulator.templates.fields :as fields]
    [com.ben-allred.app-simulator.templates.resources.http :as resources]
    [com.ben-allred.app-simulator.templates.transformations.http :as tr]
    [com.ben-allred.app-simulator.templates.views.forms.http :as http.views]
    [com.ben-allred.app-simulator.templates.views.forms.shared :as shared.views]
    [com.ben-allred.app-simulator.templates.views.simulators :as views.sim]
    [test.utils.dom :as test.dom]
    [test.utils.spies :as spies]))

(deftest ^:unit path-field-test
  (testing "(path-field)"
    (is (= [shared.views/path-field ::form tr/model->view tr/view->model]
           (http.views/path-field ::form)))))

(deftest ^:unit name-field-test
  (testing "(name-field)"
    (with-redefs [shared.views/with-attrs (spies/create (fn [attrs & _] attrs))]
      (testing "when a value is supplied for auto-focus?"
        (testing "renders the field with auto-focus"
          (let [[component attrs] (http.views/name-field ::form ::auto-focus)]
            (is (spies/called-with? shared.views/with-attrs
                                    (spies/matcher map?)
                                    ::form
                                    [:name]
                                    tr/model->view
                                    tr/view->model))
            (is (= fields/input component))
            (is (= "Name" (:label attrs)))
            (is (= ::auto-focus (:auto-focus? attrs))))))

      (testing "when no value is supplied for auto-focus?"
        (testing "defaults auto-focus to false"
          (let [[component attrs] (http.views/name-field ::form)]
            (is (spies/called-with? shared.views/with-attrs
                                    (spies/matcher map?)
                                    ::form
                                    [:name]
                                    tr/model->view
                                    tr/view->model))
            (is (= fields/input component))
            (is (= "Name" (:label attrs)))
            (is (false? (:auto-focus? attrs)))))))))

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
    (is (= [shared.views/status-field ::form tr/model->view tr/view->model]
           (http.views/status-field ::form)))))

(deftest ^:unit delay-field-test
  (testing "(delay-field)"
    (is (= [shared.views/delay-field ::form tr/model->view tr/view->model]
           (http.views/delay-field ::form)))))

(deftest ^:unit headers-field-test
  (testing "(headers-field)"
    (is (= [shared.views/headers-field ::form tr/model->view tr/view->model]
           (http.views/headers-field ::form)))))

(deftest ^:unit body-field-test
  (testing "(body-field)"
    (with-redefs [shared.views/with-attrs (spies/create (fn [v & _] (assoc v :more ::attrs)))]
      (testing "renders the form field"
        (let [[node attrs] (http.views/body-field ::form)]
          (is (spies/called-with? shared.views/with-attrs
                                  (spies/matcher any?)
                                  ::form
                                  [:response :body]
                                  tr/model->view
                                  tr/view->model))
          (is (= fields/textarea node))
          (is (= "Body" (:label attrs)))
          (is (= ::attrs (:more attrs))))))))

(deftest ^:unit method-field-test
  (testing "(method-field)"
    (is (= [shared.views/method-field ::form resources/http-methods tr/model->view tr/view->model]
           (http.views/method-field ::form)))))

(deftest ^:unit sim-edit-form*-test
  (testing "(sim-edit-form*)"
    (with-redefs [#?@(:cljs [forms/errors (spies/create)
                             forms/verified? (spies/create)
                             forms/changed? (spies/constantly true)
                             interactions/update-simulator (spies/constantly ::submit)
                             interactions/reset-simulator (spies/constantly ::reset)])]
      (testing "when rendering a form"
        (let [root (http.views/sim-edit-form* ::id ::form)
              edit-form (test.dom/query-one root :.simulator-edit)]
          (testing "renders a name field"
            (let [node (test.dom/query-one edit-form http.views/name-field)]
              (is (= [http.views/name-field ::form true] node))))

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
                         (test.dom/query-one shared.views/sync-button :.reset-button)
                         (test.dom/attrs)
                         (:on-click)
                         (= ::reset)))
                 (is (spies/called-with? interactions/reset-simulator ::form ::id))))))

        #?(:cljs
           (testing "and when submitting the form"
             (testing "and when there are verified errors"
               (spies/reset! interactions/update-simulator forms/errors forms/changed?)
               (spies/respond-with! forms/errors (constantly ::errors))
               (spies/respond-with! forms/verified? (constantly true))
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
                           (test.dom/query-one shared.views/sync-button :.save-button)
                           (test.dom/attrs)
                           (:disabled))))))

             (testing "and when there are no changes"
               (spies/reset! interactions/update-simulator forms/errors forms/verified? forms/changed?)
               (spies/respond-with! forms/changed? (constantly false))
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
                           (test.dom/query-one shared.views/sync-button :.save-button)
                           (test.dom/attrs)
                           (:disabled))))))

             (testing "and when there are no errors and changes"
               (spies/reset! interactions/update-simulator forms/errors forms/changed?)
               (let [root (http.views/sim-edit-form* ::id ::form)
                     edit-form (test.dom/query-one root :.simulator-edit)]
                 (testing "has an :on-submit attr"
                   (is (-> edit-form
                           (test.dom/query-one :.simulator-edit)
                           (test.dom/attrs)
                           (:on-submit)
                           (= ::submit))))

                 (testing "has an enabled :on-submit"
                   (is (spies/called-with? interactions/update-simulator ::form ::id)))

                 (testing "has an enabled save button"
                   (is (-> edit-form
                           (test.dom/query-one shared.views/sync-button :.save-button)
                           (test.dom/attrs)
                           (:disabled)
                           (not))))))))))))

(deftest ^:unit sim-edit-form-test
  (testing "(sim-edit-form)"
    (let [simulator {:config {:useless     ::thing
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
      (with-redefs [#?@(:cljs [form/create (spies/constantly ::form)])]
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
               (is (spies/called-with? form/create expected resources/validate-existing))))

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
                           ::data     ::456}]}]
      (with-redefs [#?@(:cljs [shared.interactions/clear-requests (spies/constantly ::clear)
                               shared.interactions/show-delete-modal (spies/constantly ::delete)])]
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
                 (is (spies/called-with? shared.interactions/clear-requests :http ::simulator-id))

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
                 (is (spies/called-with? shared.interactions/show-delete-modal ::simulator-id))))))))))

(deftest ^:unit sim-create-form*-test
  (testing "(sim-create-form*)"
    (with-redefs [nav*/path-for (spies/constantly ::href)
                  #?@(:cljs [forms/errors (spies/create)
                             forms/verified? (spies/create)
                             interactions/create-simulator (spies/constantly ::submit)])]
      (let [root (http.views/sim-create-form* ::form)
            form (test.dom/query-one root :.simulator-create)]
        #?(:cljs
           (testing "handles submit"
             (is (spies/called-with? interactions/create-simulator ::form))
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
          (is (spies/called-with? nav*/path-for :home))
          (is (-> form
                  (test.dom/query-one :.reset-button)
                  (test.dom/attrs)
                  (:href)
                  (= ::href))))

        (testing "renders a save button"
          (let [button (test.dom/query-one form shared.views/sync-button)
                attrs (test.dom/attrs button)]
            (is button)
            (is #?(:clj  (:disabled attrs)
                   :cljs (not (:disabled attrs)))))))

      #?(:cljs
         (testing "when there are verified errors"
           (spies/reset! forms/errors forms/verified? interactions/create-simulator)
           (spies/respond-with! forms/errors (constantly ::errors))
           (spies/respond-with! forms/verified? (constantly true))
           (testing "renders a disabled save button"
             (let [root (http.views/sim-create-form* ::form)]
               (is (spies/called-with? forms/errors ::form))
               (is (spies/called-with? forms/verified? ::form))
               (is (spies/called-with? interactions/create-simulator ::form))
               (is (-> root
                       (test.dom/query-one shared.views/sync-button)
                       (test.dom/attrs)
                       (:disabled))))))))))

(deftest ^:unit sim-create-form-test
  (testing "(sim-create-form)"
    (with-redefs [#?@(:cljs [form/create (spies/constantly ::form)])]
      (testing "renders the form"
        (let [component (http.views/sim-create-form)
              model {:response {:status 200
                                :body   nil}
                     :method   :http/get
                     :path     "/"
                     :delay    0}]
          #?(:cljs
             (is (spies/called-with? form/create model resources/validate-new)))
          (let [root (component)]
            (is (test.dom/contains? root [http.views/sim-create-form* #?(:clj model :cljs ::form)]))))))))

(defn run-tests []
  (t/run-tests))
