(ns com.ben-allred.clj-app-simulator.templates.transformations.file-test
  (:require [clojure.test :refer [deftest testing is are]]
            [com.ben-allred.clj-app-simulator.templates.transformations.file :as tr]
            [test.utils.spies :as spies]))

(deftest ^:unit source->model-test
  (testing "(source->model)"
    (testing "transforms values"
      (are [input expected] (-> input
                                (tr/source->model)
                                (= expected))
        {:delay    ::delay
         :response {:headers {:header-1 [::a ::b]
                              :header-2 ::c}}}
        {:delay    ::delay
         :response {:headers [[:header-1 ::a]
                              [:header-1 ::b]
                              [:header-2 ::c]]}}

        {:delay nil}
        {:delay 0}))))

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

        {:response    {:file ::file}
         :name        "   name   "
         :group       "  group  "
         :description "  description  "}
        {:response    {:file ::file}
         :name        "name"
         :group       "group"
         :description "description"}

        {:name ""}
        {:name nil}))))

(deftest ^:unit sim->model-test
  (testing "(sim->model)"
    (testing "transforms values"
      (let [spy (spies/constantly ::model)]
        (with-redefs [tr/source->model spy]
          (let [config {:things      ::things
                        :stuff       ::stuff
                        :group       ::group
                        :response    ::response
                        :delay       ::delay
                        :name        ::name
                        :description ::description}
                result (tr/sim->model {:config config})]
            (is (spies/called-with? spy {:group       ::group
                                         :response    ::response
                                         :delay       ::delay
                                         :name        ::name
                                         :description ::description}))
            (is (= ::model result))))))))

