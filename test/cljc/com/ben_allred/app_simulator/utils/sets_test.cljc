(ns com.ben-allred.app-simulator.utils.sets-test
  (:require
    [clojure.test :as t :refer [deftest is testing]]
    [com.ben-allred.app-simulator.utils.sets :as sets]))

(deftest ^:unit ordered-test
  (testing "(ordered)"
    (testing "adds and removes values"
      (let [s (conj sets/ordered :a :b :c)]
        (is (= s #{:a :b :c}))
        (is (= s [:a :b :c]))
        (is (not= s [:c :b :a]))
        (let [s (disj s :b)]
          (is (= s #{:a :c}))
          (is (= s [:a :c]))
          (is (-> s (count) (= 2)))
          (is (contains? s :c))
          (is (-> s (contains? :b) (not))))))

    (testing "handles meta data"
      (is (-> sets/ordered
              (with-meta {:a 1})
              (conj :a)
              (meta)
              (= {:a 1}))))

    (testing "is seqable"
      (is (-> sets/ordered
              (conj :a :b :c)
              (seq)
              (= [:a :b :c]))))

    (testing "is invokable"
      (let [s (conj sets/ordered :a :b :c)]
        (is (->> [:a :b :d :a :c :d :e :e :f :a :g]
                 (filter s)
                 (= [:a :b :a :c :a])))))))

(defn run-tests []
  (t/run-tests))
