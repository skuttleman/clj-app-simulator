(ns com.ben-allred.clj-app-simulator.utils.maps-test
    (:require [clojure.test :refer [deftest testing is]]
              [com.ben-allred.clj-app-simulator.utils.maps :as maps]
              [clojure.string :as string]))

(deftest ^:unit update-maybe-test
    (testing "(update-maybe)"
        (testing "calls update when value of key is not nil"
            (is (= {:a 4 :b {:c :d}} (maps/update-maybe {:a 4 :b {}} :b assoc :c :d))))
        (testing "returns map when map does not contain key"
            (is (= {:a 4 :b nil} (maps/update-maybe {:a 4 :b nil} :b assoc :c :d))))))

(deftest ^:unit map-kv-test
    (testing "(map-kv)"
        (testing "updates keys and values"
            (is (= {"KEY1" "value1" "KEY2" "value2"}
                   (maps/map-kv string/upper-case string/lower-case {"key1" "VALUE1" "kEy2" "VaLUe2"}))))))

(deftest ^:unit map-keys-test
    (testing "(map-keys)"
        (testing "updates keys and values"
            (is (= {"KEY1" "VALUE1" "KEY2" "value2"}
                   (maps/map-keys string/upper-case {"key1" "VALUE1" "kEy2" "value2"}))))))

(deftest ^:unit map-vals-test
    (testing "(map-vals)"
        (testing "updates keys and values"
            (is (= {"KEY1" "VALUE1" "key2" "VALUE2"}
                   (maps/map-vals string/upper-case {"KEY1" "VALUE1" "key2" "VaLUe2"}))))))

(deftest ^:unit update-all-test
    (testing "(update-all)"
        (testing "updates every val in map with f & f-args"
            (is (= {:a 7 :b 3 :c 49}
                   (maps/update-all {:a 10 :b 6 :c 52} - 3))))))

(deftest ^:unit ->map-test
    (testing "(->map)"
        (testing "creates map when turning symbols into keywords"
            (is (= {:a 1 :b 2} (let [a 1 b 2] (maps/->map a b)))))
        (testing "includes non-symbol values"
            (is (= {:a 1 :b 2 :c 15} (let [a 1 b 2] (maps/->map a b :c (+ 7 8))))))))

(deftest ^:unit deep-merge-test
    (testing "(deep-merge)"
        (testing "merges nested maps"
            (is (= {:a {:b {:c 1 :g 19} :d {:e {}}} :f #{4}}
                   (maps/deep-merge {:a {:b {:c 1} :d {:e 2}} :f [1 2 3]}
                                    {:a {:b {:g 19} :d {:e {}}} :f #{4}}))))))

(deftest ^:unit dissocp-test
    (testing "(dissocp)"
        (testing "dissoc's all keys where value matches predicate"
            (is (= {:a 1 :b :keyword :d nil :f 'symbol}
                   (maps/dissocp {:a 1 :b :keyword :c identity :d nil :e map :f 'symbol} fn?))))))
