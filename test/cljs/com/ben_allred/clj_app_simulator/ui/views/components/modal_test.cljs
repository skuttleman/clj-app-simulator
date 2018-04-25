(ns ^:figwheel-load com.ben-allred.clj-app-simulator.ui.views.components.modal-test
  (:require [cljs.test :as t :refer-macros [deftest testing is]]
            [com.ben-allred.clj-app-simulator.ui.views.components.modal :as modal]
            [test.utils.dom :as test.dom]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.utils.dom :as dom]))

(defn ^:private modal [state & [content title]]
  (modal/modal {:state state :content content :title title}))

(deftest ^:unit modal-test
  (testing "(modal)"
    (let [spy (spies/create)]
      (testing "adds class of state to modal-wrapper"
        (is (test.dom/query-one (modal :some-state) :.modal-wrapper.some-state)))
      (testing "has click handler to hide modal"
        (spies/reset! spy)
        (with-redefs [store/dispatch spy]
          (test.dom/simulate-event (modal :some-state) :click)
          (is (spies/called-with? spy actions/hide-modal))))
      (testing "has close-button"
        (spies/reset! spy)
        (with-redefs [store/dispatch spy]
          (test.dom/simulate-event (test.dom/query-one (modal :some-state) :.close-button) :click)
          (is (spies/called-with? spy actions/hide-modal))))
      (testing "has a modal component"
        (is (test.dom/query-one (modal :some-state) :.modal)))
      (testing "has no modal component when state is :unmounted"
        (is (not (test.dom/query-one (modal :unmounted) :.modal))))
      (testing "stops propagation when clicking modal"
        (spies/reset! spy)
        (with-redefs [dom/stop-propagation spy]
          (let [event-data {:event :data}
                modal (test.dom/query-one (modal :some-state) :.modal)]
            (test.dom/simulate-event modal :click event-data)
            (is (spies/called-with? spy event-data)))))
      (testing "has modal-title"
        (let [modal (test.dom/query-one (modal :some-state nil ::title) :.modal)
              modal-title (test.dom/query-one modal :.modal-title)]
          (is (test.dom/contains? modal-title ::title))))
      (testing "has modal-content"
        (let [modal (test.dom/query-one (modal :some-state ::content) :.modal)
              modal-content (test.dom/query-one modal :.modal-content)]
          (is (test.dom/contains? modal-content ::content)))))))

(defn run-tests [] (t/run-tests))
