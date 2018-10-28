(ns com.ben-allred.app-simulator.ui.views.components.modal-test
  (:require
    [clojure.test :as t :refer-macros [deftest is testing]]
    [com.ben-allred.app-simulator.ui.services.store.actions :as actions]
    [com.ben-allred.app-simulator.ui.services.store.core :as store]
    [com.ben-allred.app-simulator.ui.utils.dom :as dom]
    [com.ben-allred.app-simulator.ui.views.components.modal :as modal]
    [test.utils.dom :as test.dom]
    [test.utils.spies :as spies]
    [com.ben-allred.app-simulator.ui.simulators.shared.modals :as modals]))

(defn ^:private modal [state & [content title & actions]]
  (modal/modal {:state state :content content :title title :actions actions}))

(deftest ^:unit modal-test
  (testing "(modal)"
    (let [spy (spies/create)]
      (testing "adds class of state to modal-wrapper"
        (is (test.dom/query-one (modal :some-state) :.modal.some-state)))

      (testing "has click handler to hide modal"
        (spies/reset! spy)
        (with-redefs [store/dispatch spy]
          (test.dom/simulate-event (modal :some-state) :click)
          (is (spies/called-with? spy actions/hide-modal))))

      (testing "has close-button"
        (is (test.dom/query-one (modal :some-state) :.modal-close.is-large)))

      (testing "has a modal component"
        (is (test.dom/query-one (modal :some-state) :.modal)))

      (testing "has no modal component when state is :unmounted"
        (is (not (test.dom/query-one (modal :unmounted) :.modal))))

      (testing "stops propagation when clicking modal"
        (spies/reset! spy)
        (with-redefs [dom/stop-propagation spy]
          (let [event-data {:event :data}
                modal (test.dom/query-one (modal :some-state) :.modal-content)]
            (test.dom/simulate-event modal :click event-data)
            (is (spies/called-with? spy event-data)))))

      (testing "has modal-title"
        (let [modal (test.dom/query-one (modal :some-state [:empty] ::title) :.modal)
              modal-title (test.dom/query-one modal :.card-header-title)]
          (is (test.dom/contains? modal-title ::title))))

      (testing "has modal-content"
        (doseq [[k f] {:modals/confirm-delete modals/confirm-delete
                       :modals/request-modal  modals/request-modal
                       :modals/message-editor modals/message-editor
                       :modals/socket-modal   modals/socket-modal}]
          (let [modal (test.dom/query-one (modal :some-state [k ::arg]) :.modal)
                modal-content (test.dom/query-one modal :.modal-content)]
            (is (test.dom/contains? modal-content [f ::arg])))))

      (testing "when rendering action components"
        (spies/reset! spy)
        (with-redefs [store/dispatch spy]
          (let [click-spy (spies/constantly ::click)
                modal (modal :some-state [::content] ::title [:button {:on-click click-spy} "Contents"] [:button "Contents"])
                [button-1 button-2] (-> modal
                                        (test.dom/query-one :.card-footer)
                                        (test.dom/query-all :button))
                hide-modal (ffirst (spies/calls click-spy))]

            (testing "has contents"
              (is (test.dom/contains? button-1 "Contents"))
              (is (test.dom/contains? button-2 "Contents")))

            (testing "wraps hide-modal"
              (is (= ::click (:on-click (test.dom/attrs button-1)))))

            (testing "has hide-modal"
              (is (= hide-modal (:on-click (test.dom/attrs button-2)))))))))))

(defn run-tests []
  (t/run-tests))
