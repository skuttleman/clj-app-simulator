(ns com.ben-allred.clj-app-simulator.templates.simulators.shared.views-test
  (:require [clojure.test :as t :refer [deftest testing is] :include-macros true]
            [com.ben-allred.clj-app-simulator.services.forms :as forms]
            [com.ben-allred.clj-app-simulator.templates.components.form-fields :as ff]
            [com.ben-allred.clj-app-simulator.templates.simulators.shared.views :as shared.views]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.modals :as modals]
            [com.ben-allred.clj-app-simulator.utils.datetime :as dt]
            [test.utils.dom :as test.dom]
            [test.utils.spies :as spies]))

(deftest ^:unit with-attrs-test
  (testing "(with-attrs)"
    (let [assoc-spy (spies/create)
          current-model-spy (spies/constantly {:some {:path ::value}})
          errors-spy (spies/constantly {:some {:path ::errors}})
          model->view {:some {:path ::to-view}}
          view->model {:some {:path ::to-model}}]
      (with-redefs [forms/assoc-in assoc-spy
                    forms/current-model current-model-spy
                    forms/errors errors-spy]
        (testing "builds attrs"
          (let [{:keys [on-change value to-view to-model errors]}
                (shared.views/with-attrs {:some ::attrs}
                                         ::form
                                         [:some :path]
                                         model->view
                                         view->model)]
            (on-change ::new-value)
            (is (spies/called-with? assoc-spy ::form [:some :path] ::new-value))
            (is (spies/called-with? current-model-spy ::form))
            (is (= ::value value))
            (is (= ::to-view to-view))
            (is (= ::to-model to-model))
            (is (spies/called-with? errors-spy ::form))
            (is (= ::errors errors))))))))

(deftest ^:unit name-field-test
  (testing "(name-field)"
    (let [attrs-spy (spies/create identity)]
      (with-redefs [shared.views/with-attrs attrs-spy]
        (let [root (shared.views/name-field ::form ::model->view ::view->model)
              input (test.dom/query-one root ff/input)]
          (testing "has an input with attrs"
            (is (spies/called-with? attrs-spy
                                    (spies/matcher map?)
                                    ::form
                                    [:name]
                                    ::model->view
                                    ::view->model))
            (is (= "Name" (:label (test.dom/attrs input))))))))))

(deftest ^:unit group-field-test
  (testing "(group-field)"
    (let [attrs-spy (spies/create identity)]
      (with-redefs [shared.views/with-attrs attrs-spy]
        (let [root (shared.views/group-field ::form ::model->view ::view->model)
              input (test.dom/query-one root ff/input)]
          (testing "has an input with attrs"
            (is (spies/called-with? attrs-spy
                                    (spies/matcher map?)
                                    ::form
                                    [:group]
                                    ::model->view
                                    ::view->model))
            (is (= "Group" (:label (test.dom/attrs input))))))))))

(deftest ^:unit description-field-test
  (testing "(description-field)"
    (let [attrs-spy (spies/create identity)]
      (with-redefs [shared.views/with-attrs attrs-spy]
        (let [root (shared.views/description-field ::form ::model->view ::view->model)
              input (test.dom/query-one root ff/textarea)]
          (testing "has an input with attrs"
            (is (spies/called-with? attrs-spy
                                    (spies/matcher map?)
                                    ::form
                                    [:description]
                                    ::model->view
                                    ::view->model))
            (is (= "Description" (:label (test.dom/attrs input))))))))))

(deftest ^:unit path-field-test
  (testing "(path-field)"
    (let [attrs-spy (spies/create identity)]
      (with-redefs [shared.views/with-attrs attrs-spy]
        (let [root (shared.views/path-field ::form ::model->view ::view->model)
              input (test.dom/query-one root ff/input)]
          (testing "has an input with attrs"
            (is (spies/called-with? attrs-spy
                                    (spies/matcher map?)
                                    ::form
                                    [:path]
                                    ::model->view
                                    ::view->model))
            (is (= "Path" (:label (test.dom/attrs input))))))))))

(deftest ^:unit sim-details-test
  (testing "(sim-details)"
    (let [root (shared.views/sim-details {:config {:method ::method :path ::path}})]
      (testing "displays the method"
        (is (-> root
                (test.dom/query-one :.sim-card-method)
                (test.dom/contains? "METHOD"))))
      (testing "displays the path"
        (is (-> root
                (test.dom/query-one :.sim-card-path)
                (test.dom/contains? ::path)))))))

(deftest ^:unit sim-request-test
  (testing "(sim-request)"
    (let [from-now-spy (spies/constantly ::from-now)
          action-spy (spies/constantly ::action)
          dispatch-spy (spies/create)]
      (with-redefs [dt/format from-now-spy
                    actions/show-modal action-spy
                    store/dispatch dispatch-spy]
        (let [request {:timestamp ::timestamp :details ::details}
              root (shared.views/sim-request ::sim request)
              tree (test.dom/query-one root :.request)]

          (testing "when clicking the tree"
            (spies/reset! action-spy dispatch-spy)
            (test.dom/simulate-event tree :click)
            (testing "shows the modal"
              (is (spies/called-with? action-spy
                                      [modals/request-modal ::sim (assoc request :dt ::timestamp)]
                                      "Request Details"))
              (is (spies/called-with? dispatch-spy ::action))))

          (testing "displays timestamp from now"
            (is (test.dom/contains? tree ::from-now))))))))

(defn run-tests []
  (t/run-tests))
