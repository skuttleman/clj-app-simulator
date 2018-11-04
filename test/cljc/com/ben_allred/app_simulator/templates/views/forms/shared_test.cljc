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
       (with-redefs [forms/assoc-in (spies/create)
                     forms/current-model (spies/constantly {:some {:path ::value}})
                     forms/display-errors (spies/constantly {:some {:path ::errors}})
                     forms/syncing? (spies/constantly ::syncing)]
         (testing "builds attrs"
           (let [model->view {:some {:path ::to-view}}
                 view->model {:some {:path ::to-model}}
                 {:keys [on-change value to-view to-model errors disabled]}
                 (shared.views/with-attrs {:some ::attrs}
                                          ::form
                                          [:some :path]
                                          model->view
                                          view->model)]
             (on-change ::new-value)

             (is (spies/called-with? forms/assoc-in ::form [:some :path] ::new-value))
             (is (spies/called-with? forms/current-model ::form))
             (is (spies/called-with? forms/syncing? ::form))
             (is (spies/called-with? forms/display-errors ::form))

             (is (= ::value value))
             (is (= ::to-view to-view))
             (is (= ::to-model to-model))
             (is (= ::errors errors))
             (is (= ::syncing disabled))))))))

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
