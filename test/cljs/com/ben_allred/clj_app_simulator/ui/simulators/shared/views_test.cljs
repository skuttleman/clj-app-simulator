(ns com.ben-allred.clj-app-simulator.ui.simulators.shared.views-test
  (:require [cljs.test :as t :refer [deftest testing is]]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.views :as shared.views]
            [test.utils.dom :as test.dom]
            [com.ben-allred.clj-app-simulator.ui.services.forms.fields :as fields]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.utils.moment :as mo]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.modals :as modals]))

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
          (let [{:keys [on-change value to-view to-model errors some]}
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
              input (test.dom/query-one root fields/input)]
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
              input (test.dom/query-one root fields/input)]
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
              input (test.dom/query-one root fields/textarea)]
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
              input (test.dom/query-one root fields/input)]
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
    (let [moment-spy (spies/constantly ::moment)
          from-now-spy (spies/constantly ::from-now)
          action-spy (spies/constantly ::action)
          dispatch-spy (spies/create)]
      (with-redefs [mo/->moment moment-spy
                    mo/from-now from-now-spy
                    actions/show-modal action-spy
                    store/dispatch dispatch-spy]
        (let [request {:timestamp ::timestamp :details ::details}
              root (shared.views/sim-request ::sim request)
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

(defn run-tests []
  (t/run-tests))
