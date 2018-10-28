(ns com.ben-allred.app-simulator.utils.keywords-test
  (:require
    [clojure.test :as t :refer [are deftest is testing]]
    [com.ben-allred.app-simulator.utils.keywords :as keywords]))

(deftest ^:unit safe-name-test
  (testing "(safe-name)"
    (testing "calls name on keyword"
      (is (= "keyword" (keywords/safe-name :keyword))))
    (testing "returns unchanged value for any other type"
      (are [x] (= x (keywords/safe-name x))
        "a string"
        13
        nil
        {:some :map}
        [1 2 3]
        #{}
        'symbol
        #'safe-name-test))))

(deftest ^:unit join-test
  (testing "(join)"
    (testing "joins keywords"
      (is (= :joinedkeyword (keywords/join [:joined :keyword]))))
    (testing "joins with separator"
      (is (= :keywords-joined-with-dash
             (keywords/join :- [:keywords :joined :with :dash]))))
    (testing "works on non-keywords"
      (is (= :join-every-1
             (keywords/join "-" ["join" :every 1]))))))

(defn run-tests []
  (t/run-tests))
