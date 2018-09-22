(ns com.ben-allred.clj-app-simulator.ui.views.resources-test
  (:require [clojure.test :as t :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.views.resources :as resources]
            [test.utils.dom :as test.dom]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.templates.views.resources :as views.res]
            [com.ben-allred.clj-app-simulator.ui.views.components.core :as components]
            [com.ben-allred.clj-app-simulator.ui.simulators.file.interactions :as interactions]))

(deftest ^:unit resource-test
  (testing "(resource)"
    (let [delete-spy (spies/constantly ::delete)
          upload-spy (spies/constantly ::action)
          show-spy (spies/constantly ::on-click)]
      (with-redefs [actions/delete-upload delete-spy
                    interactions/replace-resource upload-spy
                    interactions/show-delete-modal show-spy]
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
              (is (spies/called-with? upload-spy ::id ::files)))))))))

(deftest ^:unit root-test
  (testing "(root)"
    (let [upload-spy (spies/constantly ::action)
          show-spy (spies/constantly ::on-click)]
      (with-redefs [interactions/upload-resources upload-spy
                    interactions/show-delete-modal show-spy]
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
              (is (spies/called-with? upload-spy ::files))))

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
