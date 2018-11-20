(ns com.ben-allred.app-simulator.templates.transformations.http-test
  (:require
    [clojure.test :as t :refer [are deftest is testing]]
    [com.ben-allred.app-simulator.templates.transformations.http :as tr]
    [test.utils.spies :as spies]))

(deftest ^:unit source->model-test
  (testing "(source->model)"
    (testing "transforms values"
      (are [input expected] (-> input
                                (tr/source->model)
                                (= expected))
        {:delay    ::delay
         :response {:body    ::body
                    :headers {:header-1 [::a ::b]
                              :header-2 ::c}}}
        {:delay    ::delay
         :response {:body    ::body
                    :headers [[:header-1 ::a]
                              [:header-1 ::b]
                              [:header-2 ::c]]}}

        {:delay nil}
        {:delay 0 :response {:body nil}}))))

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

(deftest ^:unit model->source-test
  (testing "(model->source)"
    (testing "transforms values"
      (are [input expected] (-> input
                                (tr/model->source)
                                (= expected))
        {:delay    ::delay
         :response {:headers [[:header-1 "  a  "]
                              [:header-1 "  b  "]
                              [:header-2 "  c  "]]}}
        {:delay    ::delay
         :response {:headers {:header-1 ["a" "b"]
                              :header-2 "c"}}}

        {:response    {:body "   body   "}
         :name        "   name   "
         :group       "  group  "
         :description "  description  "}
        {:response    {:body "body"}
         :name        "name"
         :group       "group"
         :description "description"}

        {:name ""}
        {:name nil}))))

(defn run-tests []
  (t/run-tests))
