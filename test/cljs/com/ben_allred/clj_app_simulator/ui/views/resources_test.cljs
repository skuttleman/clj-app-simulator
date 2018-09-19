(ns com.ben-allred.clj-app-simulator.ui.views.resources-test
  (:require [clojure.test :as t :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.modals :as modals]
            [com.ben-allred.clj-app-simulator.ui.views.resources :as resources]
            [test.utils.dom :as test.dom]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.templates.views.resources :as views.res]
            [com.ben-allred.clj-app-simulator.ui.views.components.core :as components]))

(deftest ^:unit show-delete-modal-test
  (testing "(show-delete-modal)"
    (let [dispatch-spy (spies/create)
          show-spy (spies/constantly ::action)
          hide-spy (spies/create)]
      (with-redefs [store/dispatch dispatch-spy
                    actions/show-modal show-spy]
        ((resources/show-delete-modal ::title ::msg ::on-click) ::ignored)
        (let [[modal title delete-btn cancel-btn] (first (spies/calls show-spy))]
          (testing "dispatches the action"
            (is (spies/called-with? dispatch-spy ::action)))

          (testing "has a modal"
            (is (= modal [modals/confirm-delete ::msg])))

          (testing "has a title"
            (is (= title ::title)))

          (testing "has a delete button"
            (let [on-click (-> delete-btn
                               (test.dom/query-one :.delete-button)
                               (test.dom/attrs)
                               (:on-click))]
              ((on-click hide-spy) ::ignored)
              (is (spies/called-with? hide-spy))
              (is (spies/called-with? dispatch-spy ::on-click))))

          (testing "has a cancel button"
            (is (test.dom/query-one cancel-btn :.cancel-button))))))))

(deftest ^:unit resource-test
  (testing "(resource)"
    (let [dispatch-spy (spies/create)
          delete-spy (spies/constantly ::delete)
          upload-spy (spies/constantly ::action)
          show-spy (spies/constantly ::on-click)]
      (with-redefs [store/dispatch dispatch-spy
                    actions/delete-upload delete-spy
                    actions/upload-replace upload-spy
                    resources/show-delete-modal show-spy]
        (let [upload {:id ::id :other :stuff}
              [_ attrs arg upload-btn] (-> upload
                                           (resources/resource)
                                           (test.dom/query-one views.res/resource))]
          (testing "has an :on-click attr"
            (is (spies/called-with? delete-spy ::id))
            (is (spies/called-with? show-spy "Delete Resource" "this resource" ::delete))
            (is (= ::on-click (:on-click attrs))))

          (testing "passes the upload value"
            (is (= arg upload)))

          (testing "has an upload control"
            (let [[_ {:keys [multiple on-change]}] (test.dom/query-one upload-btn components/upload)]
              (is (false? multiple))
              (on-change ::files)
              (is (spies/called-with? upload-spy ::id ::files))
              (is (spies/called-with? dispatch-spy ::action)))))))))

(deftest ^:unit root-test
  (testing "(root)"
    (let [dispatch-spy (spies/create)
          upload-spy (spies/constantly ::action)
          show-spy (spies/constantly ::on-click)]
      (with-redefs [store/dispatch dispatch-spy
                    actions/upload upload-spy
                    resources/show-delete-modal show-spy]
        (let [[_ attrs upload-btn res uploads] (-> (resources/root [::upload])
                                                       (test.dom/query-one views.res/resources))]
          (testing "has an :on-click attr"
            (is (spies/called-with? show-spy "Delete All Resources" "all resources" actions/delete-uploads))
            (is (= ::on-click (:on-click attrs)))
            (is (not (:disabled attrs))))

          (testing "has an upload control"
            (let [[_ {:keys [multiple on-change]}] (test.dom/query-one upload-btn components/upload)]
              (is (nil? multiple))
              (on-change ::files)
              (is (spies/called-with? upload-spy ::files))
              (is (spies/called-with? dispatch-spy ::action))))

          (testing "passes the resource component"
            (is (= res resources/resource)))

          (testing "passes the uploads"
            (is (= uploads [::upload])))

          (testing "when the uploads are empty"
            (let [[_ attrs] (-> (resources/root [])
                                (test.dom/query-one views.res/resources))]
              (testing "disables the control"
                (is (:disabled attrs))))))))))

(defn run-tests []
  (t/run-tests))
