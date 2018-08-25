(ns com.ben-allred.clj-app-simulator.utils.colls-test
  (:require [clojure.test :refer [deftest testing is are]]
            [com.ben-allred.clj-app-simulator.utils.colls :as colls]))

(deftest ^:unit force-sequential-test
  (testing "(force-sequential)"
    (are [input expected] (= (colls/force-sequential input) expected)
      nil nil
      1 [1]
      :keyword [:keyword]
      [1 2 3] [1 2 3]
      () ()
      #{1 2 3} [#{1 2 3}]
      {:a 1 :b 2} [{:a 1 :b 2}])))

(deftest ^:unit replace-by-test
  (testing "(replace-by)"
    (testing "replaces item in coll"
      (are [f value coll expected] (= (colls/replace-by f value coll)
                                      expected)
        seq [] [{} {:a 1} [7]] [[] {:a 1} [7]]
        not false [nil true :true] [false true :true]
        :a {:a 1 :b 2} [{:a 2 :b 1} {:a 1 :b 22} {:a 22 :b 123}] [{:a 2 :b 1} {:a 1 :b 2} {:a 22 :b 123}]))

    (testing "replaces multiple items in coll"
      (are [f value coll expected] (= (colls/replace-by f value coll)
                                      expected)
        seq [] [{} () [1 2 3] #{}] [[] [] [1 2 3] []]
        not true [:a nil {} :true false] [true nil true true false]
        :a {:a 1 :b 8} [{:a 2 :b 88} {:a 1 :b 2} {:a 1 :b 12}] [{:a 2 :b 88} {:a 1 :b 8} {:a 1 :b 8}]))

    (testing "returns coll unchanged"
      (is (= [1 2 3 4 5] (colls/replace-by identity 7 [1 2 3 4 5]))))))
