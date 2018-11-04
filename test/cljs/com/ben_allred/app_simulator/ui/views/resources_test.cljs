(ns com.ben-allred.app-simulator.ui.views.resources-test
  (:require
    [clojure.test :as t :refer [deftest is testing]]
    [com.ben-allred.app-simulator.templates.views.resources :as views.res]
    [com.ben-allred.app-simulator.ui.services.store.actions :as actions]
    [com.ben-allred.app-simulator.ui.simulators.file.interactions :as interactions]
    [com.ben-allred.app-simulator.ui.views.components.core :as components]
    [com.ben-allred.app-simulator.ui.views.resources :as resources]
    [test.utils.dom :as test.dom]
    [test.utils.spies :as spies]
    [com.ben-allred.app-simulator.ui.services.forms.core :as forms]))

(deftest ^:unit resource-test
  (testing "(resource)"
    (with-redefs [actions/delete-upload (spies/constantly ::delete)
                  interactions/replace-resource (spies/constantly ::action)
                  interactions/show-delete-modal (spies/constantly ::on-click)
                  forms/create (constantly ::form)
                  forms/syncing? (spies/constantly ::syncing)]
      (let [upload {:id ::id :other :stuff}
            [_ attrs arg upload-btn] (-> upload
                                         ((resources/resource ::ignored))
                                         (test.dom/query-one views.res/resource))]
        (testing "has an :on-click attr"
          (is (spies/called-with? actions/delete-upload ::id))
          (is (spies/called-with? interactions/show-delete-modal "Delete Resource" "this resource" ::delete))
          (is (= ::on-click (:on-click attrs)))
          (is (= ::syncing (:disabled attrs))))

        (testing "passes the upload value"
          (is (= arg upload)))

        (testing "has an upload control"
          (let [attrs (-> upload-btn
                          (test.dom/query-one components/upload)
                          (test.dom/attrs))]
            (is (:single? attrs))
            (is (spies/called-with? interactions/replace-resource ::form ::id))
            (is (= ::action (:on-change attrs)))
            (is (= ::syncing ((:sync-fn attrs))))
            (is (= "Replace" (:static-content attrs)))
            (is (= "Replacing" (:persisting-content attrs)))))))))

(deftest ^:unit root-test
  (testing "(root)"
    (with-redefs [interactions/upload-resources (spies/constantly ::action)
                  interactions/show-delete-modal (spies/constantly ::on-click)
                  forms/create (constantly ::form)
                  forms/syncing? (spies/constantly ::syncing)]
      (let [root (resources/root ::ignored)
            [_ attrs upload-btn res resources] (-> (root [::resource])
                                                   (test.dom/query-one views.res/resources))]
        (testing "has an :on-click attr"
          (is (spies/called-with? interactions/show-delete-modal "Delete All Resources" "all resources" actions/delete-uploads))
          (is (= ::on-click (:on-click attrs)))
          (is (= ::syncing (:disabled attrs))))

        (testing "has an upload control"
          (let [attrs (-> upload-btn
                          (test.dom/query-one components/upload)
                          (test.dom/attrs))]
            (is (= ::action (:on-change attrs)))
            (is (spies/called-with? interactions/upload-resources ::form))
            (is (= ::syncing ((:sync-fn attrs))))
            (is (= "Upload" (:static-content attrs)))
            (is (= "Uploading" (:persisting-content attrs)))))

        (testing "passes the resource component"
          (is (= res resources/resource)))

        (testing "passes the uploads"
          (is (= resources [::resource])))

        (testing "when the uploads are empty"
          (let [[_ attrs] (-> (root [])
                              (test.dom/query-one views.res/resources))]
            (testing "disables the control"
              (is (:disabled attrs)))))))))

(defn run-tests []
  (t/run-tests))
