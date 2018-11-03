(ns com.ben-allred.app-simulator.templates.views.forms.shared-test
  (:require
    #?@(:cljs [[com.ben-allred.app-simulator.ui.services.forms.core :as forms]
               [com.ben-allred.app-simulator.ui.simulators.shared.interactions :as interactions]])
    [clojure.test :as t :refer [deftest is testing]]
    [com.ben-allred.app-simulator.templates.fields :as fields]
    [com.ben-allred.app-simulator.templates.views.forms.shared :as shared.views]
    [com.ben-allred.app-simulator.utils.dates :as dates]
    [test.utils.dom :as test.dom]
    [test.utils.spies :as spies]))

(deftest ^:unit with-attrs-test
  #?(:cljs
     (testing "(with-attrs)"
       (let [assoc-spy (spies/create)
             current-model-spy (spies/constantly {:some {:path ::value}})
             errors-spy (spies/constantly {:some {:path ::errors}})
             sync-spy (spies/constantly ::syncing)
             model->view {:some {:path ::to-view}}
             view->model {:some {:path ::to-model}}]
         (with-redefs [forms/assoc-in assoc-spy
                       forms/current-model current-model-spy
                       forms/display-errors errors-spy
                       forms/syncing? sync-spy]
           (testing "builds attrs"
             (let [{:keys [on-change value to-view to-model errors disabled]}
                   (shared.views/with-attrs {:some ::attrs}
                                            ::form
                                            [:some :path]
                                            model->view
                                            view->model)]
               (on-change ::new-value)

               (is (spies/called-with? assoc-spy ::form [:some :path] ::new-value))
               (is (spies/called-with? current-model-spy ::form))
               (is (spies/called-with? sync-spy ::form))
               (is (spies/called-with? errors-spy ::form))

               (is (= ::value value))
               (is (= ::to-view to-view))
               (is (= ::to-model to-model))
               (is (= ::errors errors))
               (is (= ::syncing disabled)))))))))

(deftest ^:unit group-field-test
  (testing "(group-field)"
    (let [attrs-spy (spies/create (comp first list))]
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
    (let [attrs-spy (spies/create (comp first list))]
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

(deftest ^:unit sim-request-test
  (testing "(sim-request)"
    (let [interaction-spy (spies/constantly ::click)
          format-spy (spies/constantly ::formatted)]
      (with-redefs [#?@(:cljs [interactions/show-request-modal interaction-spy])
                    dates/format format-spy]
        (let [request {:timestamp ::timestamp :details ::details}
              root (shared.views/sim-request ::sim request)
              tree (test.dom/query-one root :.request)]

          (testing "displays formatted timestamp"
            (is (spies/called-with? format-spy ::timestamp))
            (is (test.dom/contains? tree ::formatted)))

          #?(:cljs
             (testing "has a click handler"
               (is (-> tree
                       (test.dom/attrs)
                       (:on-click)
                       (= ::click)))
               (is (spies/called-with? interaction-spy ::sim request)))))))))

(defn run-tests []
  (t/run-tests))
