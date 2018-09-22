(ns com.ben-allred.clj-app-simulator.utils.query-params-test
  (:require [clojure.test :as t :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.utils.query-params :as qp]))

(deftest ^:unit parse-test
  (testing "(parse)"
    (testing "parses query-string"
      (is (= {:a "b" :c "d" :e true}
             (qp/parse "a=b&c=d&e"))))

    (testing "handles nil"
      (is (= {} (qp/parse nil))))))

(deftest ^:unit stringify-test
  (testing "(stringify)"
    (testing "stringifies query params"
      (is (= "a=b&c=d&e=true"
             (qp/stringify [[:a "b"] [:c :d] [:e true]]))))

    (testing "handles nil"
      (is (= "" (qp/stringify nil))))))

(defn run-tests []
  (t/run-tests))
