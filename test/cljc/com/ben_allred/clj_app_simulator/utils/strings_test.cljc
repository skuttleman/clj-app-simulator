(ns com.ben-allred.clj-app-simulator.utils.strings-test
    (:require [clojure.test :refer [deftest testing is are]]
              [com.ben-allred.clj-app-simulator.utils.strings :as strings]))

(deftest trim-to-nil-test
    (testing "(trim-to-nil)"
        (testing "trims strings"
            (is (= "trimmed" (strings/trim-to-nil "   trimmed "))))
        (testing "returns nil when string is only white space"
            (is (nil? (strings/trim-to-nil "  \t\n "))))
        (testing "returns nil when value is nil"
            (is (nil? (strings/trim-to-nil nil))))))

(deftest maybe-pr-str-test
    (testing "(maybe-pr-str)"
        (testing "returns strings unchanged"
            (is (= "a string" (strings/maybe-pr-str "a string"))))
        (testing "resturns pr-str'ed values"
            (are [v s] (is (= s (strings/maybe-pr-str v)))
                {:a #{1}} "{:a #{1}}"
                ["a" \b 'c] "[\"a\" \\b c]"))))
