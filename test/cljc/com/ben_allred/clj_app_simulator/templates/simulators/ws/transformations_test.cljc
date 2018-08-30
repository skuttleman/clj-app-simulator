(ns com.ben-allred.clj-app-simulator.templates.simulators.ws.transformations-test
  (:require [clojure.test :refer [deftest testing is are]]
            [com.ben-allred.clj-app-simulator.templates.simulators.ws.transformations :as tr]
            [com.ben-allred.clj-app-simulator.utils.strings :as strings]))

(deftest ^:unit model->view-test
  (testing "(model->view)"
    (testing "converts the path to a string"
      (is (= str (:path tr/model->view))))

    (testing "converts keywords to strings"
      (let [method (:method tr/model->view)]
        (is (= "thingy" (method :thingy)))))))

(deftest ^:unit view->model-test
  (testing "(view->model)"
    (testing "trims the path to nil"
      (is (= strings/trim-to-nil (:path tr/view->model))))

    (testing "converts the method to a keyword"
      (is (= keyword (:method tr/view->model))))))

(deftest ^:unit model->source-test
  (testing "(model->source)"
    (are [m expected] (= expected (tr/model->source m))
      {:name "   " :group "" :description "\t\n"} {:name nil :group nil :description nil}
      {:name "\nname "} {:name "name"}
      {:group "a group "} {:group "a group"}
      {:description "   \n  description"} {:description "description"})))

(deftest ^:unit sim->model-test
  (testing "(sim->model)"
    (is (= {:group ::group :name ::name :description ::description}
           (tr/sim->model {:config {:group ::group
                                    :description ::description
                                    :name ::name
                                    :other ::values}})))))
