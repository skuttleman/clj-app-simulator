(ns com.ben-allred.app-simulator.templates.views.forms.shared-test
  (:require
    #?@(:cljs [[com.ben-allred.app-simulator.ui.services.forms.core :as forms]
               [com.ben-allred.app-simulator.ui.simulators.shared.interactions :as interactions]])
    [clojure.test :as t :refer [deftest is testing]]
    [com.ben-allred.app-simulator.templates.fields :as fields]
    [com.ben-allred.app-simulator.templates.views.forms.shared :as shared.views]
    [com.ben-allred.app-simulator.utils.dates :as dates]
    [test.utils.dom :as test.dom]
    [test.utils.spies :as spies]
    [com.ben-allred.app-simulator.templates.resources.shared :as shared.resources]))

(deftest ^:unit with-attrs-test
  #?(:cljs
     (testing "(with-attrs)"
       (with-redefs [forms/errors (spies/constantly {:some {:path ::errors}})
                     forms/syncing? (spies/constantly true)
                     forms/verified? (spies/constantly true)
                     forms/touched? (spies/constantly ::touched)
                     forms/assoc-in! (spies/create)]
         (testing "builds attrs"
           (let [form (reify
                        IDeref
                        (-deref [_]
                          {:some {:path ::value}}))
                 model->view {:some {:path ::to-view}}
                 view->model {:some {:path ::to-model}}
                 {:keys [disabled errors on-change to-model to-view touched? value]}
                 (shared.views/with-attrs {:some ::attrs}
                                          form
                                          [:some :path]
                                          model->view
                                          view->model)]
             (on-change ::new-value)

             (is (spies/called-with? forms/syncing? form))
             (is (spies/called-with? forms/errors form))

             (is (= ::value value))
             (is (= ::to-view to-view))
             (is (= ::to-model to-model))
             (is (= ::errors errors))
             (is (= ::touched touched?))
             (is disabled)))))))

(deftest ^:unit path-field-test
  (testing "(path-field)"
    (with-redefs [shared.views/with-attrs (spies/create (fn [attrs & _] attrs))]
      (let [[component attrs] (shared.views/path-field ::form ::model->view ::view->model)]
        (is (spies/called-with? shared.views/with-attrs
                                (spies/matcher map?)
                                ::form
                                [:path]
                                ::model->view
                                ::view->model))
        (is (= fields/input component))
        (is (= "Path" (:label attrs)))))))

(deftest ^:unit group-field-test
  (testing "(group-field)"
    (with-redefs [shared.views/with-attrs (spies/create (comp first list))]
      (let [root (shared.views/group-field ::form ::model->view ::view->model)
            input (test.dom/query-one root fields/input)]
        (testing "has an input with attrs"
          (is (spies/called-with? shared.views/with-attrs
                                  (spies/matcher map?)
                                  ::form
                                  [:group]
                                  ::model->view
                                  ::view->model))
          (is (= "Group" (:label (test.dom/attrs input)))))))))

(deftest ^:unit description-field-test
  (testing "(description-field)"
    (with-redefs [shared.views/with-attrs (spies/create (comp first list))]
      (let [root (shared.views/description-field ::form ::model->view ::view->model)
            input (test.dom/query-one root fields/textarea)]
        (testing "has an input with attrs"
          (is (spies/called-with? shared.views/with-attrs
                                  (spies/matcher map?)
                                  ::form
                                  [:description]
                                  ::model->view
                                  ::view->model))
          (is (= "Description" (:label (test.dom/attrs input)))))))))

(deftest ^:unit status-field-test
  (testing "(status-field)"
    (with-redefs [shared.views/with-attrs (spies/create (fn [v & _] (assoc v :more ::attrs)))]
      (testing "renders the form field"
        (let [[node attrs resource] (shared.views/status-field ::form ::model->view ::view->model)]
          (is (spies/called-with? shared.views/with-attrs
                                  (spies/matcher any?)
                                  ::form
                                  [:response :status]
                                  ::model->view
                                  ::view->model))
          (is (= fields/select node))
          (is (= "Status" (:label attrs)))
          (is (= ::attrs (:more attrs)))
          (is (= shared.resources/statuses resource)))))))

(deftest ^:unit delay-field-test
  (testing "(delay-field)"
    (with-redefs [shared.views/with-attrs (spies/create (fn [v & _] (assoc v :more ::attrs)))]
      (testing "renders the form field"
        (let [[node attrs] (shared.views/delay-field ::form ::model->view ::view->model)]
          (is (spies/called-with? shared.views/with-attrs
                                  (spies/matcher any?)
                                  ::form
                                  [:delay]
                                  ::model->view
                                  ::view->model))
          (is (= fields/input node))
          (is (= "Delay (ms)" (:label attrs)))
          (is (= ::attrs (:more attrs))))))))

(deftest ^:unit headers-field-test
  (testing "(headers-field)"
    (with-redefs [shared.views/with-attrs (spies/create (fn [v & _] (assoc v :more ::attrs)))]
      (testing "renders the form field"
        (let [[node attrs header] (shared.views/headers-field ::form ::model->view ::view->model)]
          (is (spies/called-with? shared.views/with-attrs
                                  (spies/matcher any?)
                                  ::form
                                  [:response :headers]
                                  ::model->view
                                  ::view->model))
          (is (= fields/multi node))
          (is (= fields/header header))
          (is (= "Headers" (:label attrs)))
          (is (= ::attrs (:more attrs)))
          (is (= "header-1" ((:key-fn attrs) [1 :value])))
          (is (= ["" ""] ((:new-fn attrs) ::ignored))))))))

(deftest ^:unit method-field-test
  (testing "(method-field)"
    (with-redefs [shared.views/with-attrs (spies/create (fn [v & _] (assoc v :more ::attrs)))]
      (testing "renders the form field"
        (let [[node attrs resource] (shared.views/method-field ::form ::resource ::model->view ::view->model)]
          (is (spies/called-with? shared.views/with-attrs
                                  (spies/matcher any?)
                                  ::form
                                  [:method]
                                  ::model->view
                                  ::view->model))
          (is (= fields/select node))
          (is (= "HTTP Method" (:label attrs)))
          (is (true? (:auto-focus? attrs)))
          (is (= ::attrs (:more attrs)))
          (is (= ::resource resource)))))))

(deftest ^:unit sim-request-test
  (testing "(sim-request)"
    (with-redefs [#?@(:cljs [interactions/show-request-modal (spies/constantly ::click)])
                  dates/format (spies/constantly ::formatted)]
      (let [request {:timestamp ::timestamp :details ::details}
            root (shared.views/sim-request ::sim request)
            tree (test.dom/query-one root :.request)]

        (testing "displays formatted timestamp"
          (is (spies/called-with? dates/format ::timestamp))
          (is (test.dom/contains? tree ::formatted)))

        #?(:cljs
           (testing "has a click handler"
             (is (-> tree
                     (test.dom/attrs)
                     (:on-click)
                     (= ::click)))
             (is (spies/called-with? interactions/show-request-modal ::sim request))))))))

(defn run-tests []
  (t/run-tests))
