(ns com.ben-allred.app-simulator.templates.views.forms.file-test
  (:require
    #?@(:cljs [[com.ben-allred.app-simulator.ui.services.forms.core :as forms]
               [com.ben-allred.app-simulator.ui.services.forms.standard :as form]
               [com.ben-allred.app-simulator.ui.services.store.actions :as actions]
               [com.ben-allred.app-simulator.ui.services.store.core :as store]
               [com.ben-allred.app-simulator.ui.simulators.file.interactions :as interactions]
               [com.ben-allred.app-simulator.ui.simulators.shared.interactions :as shared.interactions]
               [com.ben-allred.app-simulator.ui.utils.dom :as dom]])
    [clojure.test :as t :refer [deftest is testing]]
    [com.ben-allred.app-simulator.services.navigation :as nav*]
    [com.ben-allred.app-simulator.templates.fields :as fields]
    [com.ben-allred.app-simulator.templates.resources.file :as resources]
    [com.ben-allred.app-simulator.templates.transformations.file :as tr]
    [com.ben-allred.app-simulator.templates.views.forms.file :as file.views]
    [com.ben-allred.app-simulator.templates.views.forms.shared :as shared.views]
    [com.ben-allred.app-simulator.templates.views.simulators :as views.sim]
    [test.utils.dom :as test.dom]
    [test.utils.spies :as spies]))

(deftest ^:unit path-field-test
  (testing "(path-field)"
    (is (= [shared.views/path-field ::form tr/model->view tr/view->model]
           (file.views/path-field ::form)))))

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
    (is (= [shared.views/status-field ::form tr/model->view tr/view->model]
           (file.views/status-field ::form)))))

(deftest ^:unit delay-field-test
  (testing "(delay-field)"
    (is (= [shared.views/delay-field ::form tr/model->view tr/view->model]
           (file.views/delay-field ::form)))))

(deftest ^:unit headers-field-test
  (testing "(headers-field)"
    (is (= [shared.views/headers-field ::form tr/model->view tr/view->model]
           (file.views/headers-field ::form)))))

(deftest ^:unit method-field-test
  (testing "(method-field)"
    (is (= [shared.views/method-field ::form  resources/file-methods tr/model->view tr/view->model]
           (file.views/method-field ::form)))))

(deftest ^:unit name-field-test
  (testing "(name-field)"
    (with-redefs [shared.views/with-attrs (spies/create (fn [attrs & _] attrs))]
      (testing "when a value is supplied for auto-focus?"
        (testing "renders the field with auto-focus"
          (let [[component attrs] (file.views/name-field ::form ::auto-focus)]
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
          (let [[component attrs] (file.views/name-field ::form)]
            (is (spies/called-with? shared.views/with-attrs
                                    (spies/matcher map?)
                                    ::form
                                    [:name]
                                    tr/model->view
                                    tr/view->model))
            (is (= fields/input component))
            (is (= "Name" (:label attrs)))
            (is (false? (:auto-focus? attrs)))))))))

(deftest ^:unit file-field-test
  (testing "(file-field)"
    (with-redefs [shared.views/with-attrs (spies/create (fn [v & _] (assoc v :more ::attrs)))]
      (testing "renders the form field"
        (let [uploads [{:id 111 :filename ::filename2}
                       {:id 222 :filename ::filename}
                       {:id 333 :filename ::filename1}]
              [node attrs resource] (file.views/file-field ::form uploads)]
          (is (spies/called-with? shared.views/with-attrs
                                  (spies/matcher any?)
                                  ::form
                                  [:response :file]
                                  tr/model->view
                                  tr/view->model))
          (is (= fields/select node))
          (is (= "File" (:label attrs)))
          (is (= ::attrs (:more attrs)))
          (is (= [[222 ::filename]
                  [333 ::filename1]
                  [111 ::filename2]]
                 resource)))))))

(deftest ^:unit sim-edit-form*-test
  (testing "(sim-edit-form*)"
    (with-redefs [#?@(:cljs [forms/changed? (constantly nil)
                             forms/verified? (constantly true)
                             forms/errors (constantly nil)
                             dom/prevent-default (constantly nil)
                             actions/update-simulator (constantly ::action)
                             store/dispatch (constantly nil)])]
      (testing "when rendering a form"
        (let [root (file.views/sim-edit-form* ::id ::form ::uploads)
              edit-form (test.dom/query-one root :.simulator-edit)]
          (testing "renders a name field"
            (let [node (test.dom/query-one edit-form file.views/name-field)]
              (is (= [file.views/name-field ::form true] node))))

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
              (is (= [file.views/file-field ::form ::uploads] node)))))))))

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
        (let [root (file.views/sim-edit-form simulator [{:id 1 :data ::data-1} {:id 2 :data ::data-2}])
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
            (is (= [file.views/sim-edit-form*
                    ::id
                    #?(:clj expected :cljs ::form)
                    [{:id 1 :data ::data-1} {:id 2 :data ::data-2}]]
                   (root simulator [{:id 1 :data ::data-1} {:id 2 :data ::data-2}])))))))))

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
        (let [root (file.views/sim sim ::uploads)]
          (testing "displays sim-details"
            (let [details (test.dom/query-one root views.sim/sim-details)]
              (is (= [views.sim/sim-details sim] details))))

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

          #?(:cljs
             (testing "has a button to clear requests"
               (let [button (test.dom/query-one root :.button.clear-button)]
                 (is (= ::clear (:on-click (test.dom/attrs button))))
                 (is (spies/called-with? shared.interactions/clear-requests :file ::simulator-id))

                 (testing "when there are no requests"
                   (testing "is not disabled"
                     (is (not (:disabled (test.dom/attrs button))))))

                 (testing "when there are no requests"
                   (let [root (file.views/sim {} ::uploads)
                         button (test.dom/query-one root :.button.clear-button)]
                     (testing "is disabled"
                       (is (:disabled (test.dom/attrs button)))))))))

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
      (let [root (file.views/sim-create-form* ::form ::uploads)
            form (test.dom/query-one root :.simulator-create)]
        #?(:cljs
           (testing "handles submit"
             (is (spies/called-with? interactions/create-simulator ::form))
             (is (-> form
                     (test.dom/attrs)
                     (:on-submit)
                     (= ::submit)))))

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
          (is (spies/called-with? nav*/path-for :home))
          (is (-> form
                  (test.dom/query-one :.reset-button)
                  (test.dom/attrs)
                  (:href)
                  (= ::href))))

        (let [button (test.dom/query-one form :.save-button)
              attrs (test.dom/attrs button)]
          (testing "renders a save button"
            (is button)
            (is #?(:clj  (:disabled attrs)
                   :cljs (not (:disabled attrs)))))))

      #?(:cljs
         (testing "when there are verified errors"
           (spies/reset! interactions/create-simulator forms/verified? forms/errors)
           (spies/respond-with! forms/verified? (constantly true))
           (spies/respond-with! forms/errors (constantly :errors))
           (testing "disables the save button"
             (let [root (file.views/sim-create-form* ::form ::uploads)]
               (is (spies/called-with? forms/verified? ::form))
               (is (-> root
                       (test.dom/query-one :.save-button)
                       (test.dom/attrs)
                       (:disabled))))))))))

(deftest ^:unit sim-create-form-test
  (testing "(sim-create-form)"
    (with-redefs [#?@(:cljs [form/create (spies/constantly ::form)])]
      (testing "renders the form"
        (let [component (file.views/sim-create-form ::uploads)
              model {:response {:status 200}
                     :method   :file/get
                     :path     "/"
                     :delay    0}]
          #?(:cljs
             (is (spies/called-with? form/create model resources/validate-new)))
          (let [root (component ::uploads)]
            (is (test.dom/contains? root [file.views/sim-create-form* #?(:clj model :cljs ::form) ::uploads]))))))))

(defn run-tests []
  (t/run-tests))
