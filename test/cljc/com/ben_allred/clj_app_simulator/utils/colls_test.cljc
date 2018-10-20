(ns com.ben-allred.clj-app-simulator.utils.colls-test
  (:refer-clojure :exclude [list?])
  (:require
    [clojure.test :as t :refer [are deftest is testing]]
    [com.ben-allred.clj-app-simulator.utils.colls :as colls]))


(def ^:private list? (some-fn clojure.core/list? seq?))

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

(deftest ^:unit prepend-test
  (testing "(prepend)"
    (testing "works on lists and vectors"
      (are [coll x expected] (= (colls/prepend coll x) expected)
        [] 1 [1]
        () 1 [1]
        nil 1 [1]
        '(1 2 3) 0 [0 1 2 3]
        [1 2 3] 0 [0 1 2 3]))

    (testing "retains type"
      (is (vector? (colls/prepend [1 2 3] 0)))
      (is (list? (colls/prepend '(1 2 3) 0))))

    (testing "can be used as a transducer"
      (is (= (transduce (comp (map inc) (colls/prepend -20) (map dec) (take 10)) conj (range))
             [-21 0 1 2 3 4 5 6 7 8])))))

(deftest ^:unit append-test
  (testing "(append)"
    (testing "works on lists and vectors"
      (are [coll x expected] (= (colls/append coll x) expected)
        [] 1 [1]
        () 1 [1]
        nil 1 [1]
        '(1 2 3) 0 [1 2 3 0]
        [1 2 3] 0 [1 2 3 0]))

    (testing "retains type"
      (is (vector? (colls/append [1 2 3] 0)))
      (is (list? (colls/append '(1 2 3) 0))))

    (testing "can be used as a transducer"
      (is (= (transduce (comp (map inc) (colls/append -20) (map dec) (take 10)) conj (range))
             [0 1 2 3 4 5 6 7 8 9]))
      (is (= (transduce (comp (map inc) (colls/append -20) (map dec)) conj (range 9))
             [0 1 2 3 4 5 6 7 8 -21])))))

(defn run-tests []
  (t/run-tests))
