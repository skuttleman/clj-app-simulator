(ns com.ben-allred.app-simulator.templates.transformations.shared-test
  (:require
    [clojure.test :as t :refer [are deftest is testing]]
    [com.ben-allred.app-simulator.templates.transformations.shared :as tr]))

(deftest ^:unit model->view-test
  (testing "(model->view)"
    (testing "transforms values"
      (are [path input expected] (= expected
                                    ((get-in tr/model->view path) input))
        [:delay] 99 "99"
        [:delay] nil ""
        [:response :status] 99 "99"
        [:response :headers] [:header-thing ::value] ["Header-Thing" ::value]
        [:response :headers] [(keyword "") ""] ["" ""]))))

(deftest ^:unit view->model-test
  (testing "(view->model)"
    (testing "transforms values"
      (are [path input expected] (= expected
                                    ((get-in tr/view->model path) input))
        [:delay] "99" 99
        [:delay] "not-a-number" "not-a-number"
        [:response :status] "99" 99
        [:response :headers] ["Header-Thing" ::value] [:header-thing ::value]
        [:response :headers] ["" ""] [(keyword "") ""]))))

(deftest ^:unit headers->source-test
  (testing "(headers->source)"
    (testing "transforms values"
      (are [input expected] (-> input
                                ((:headers tr/headers->source))
                                (= expected))
        [[:header-1 "  a  "]
         [:header-1 "  b  "]
         [:header-2 "  c  "]]
        {:header-1 ["a" "b"]
         :header-2 "c"}

        nil
        {}))))

(deftest ^:unit model->source-test
  (testing "(model->source)"
    (testing "transforms values"
      (are [key input expected] (-> input
                                    ((get tr/model->source key))
                                    (= expected))
        :name "   name   " "name"
        :group "  group  " "group"
        :description "  description  " "description"
        :name "   " nil
        :group "\t" nil
        :description "" nil))))

(defn run-tests []
  (t/run-tests))
