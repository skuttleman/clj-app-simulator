(ns com.ben-allred.app-simulator.utils.strings-test
  (:require
    [clojure.test :as t :refer [are deftest is testing]]
    [com.ben-allred.app-simulator.utils.strings :as strings]))

(deftest ^:unit trim-to-nil-test
  (testing "(trim-to-nil)"
    (testing "trims strings"
      (is (= "trimmed" (strings/trim-to-nil "   trimmed "))))
    (testing "returns nil when string is only white space"
      (is (nil? (strings/trim-to-nil "  \t\n "))))
    (testing "returns nil when value is nil"
      (is (nil? (strings/trim-to-nil nil))))))

(deftest ^:unit maybe-pr-str-test
  (testing "(maybe-pr-str)"
    (testing "returns strings unchanged"
      (is (= "a string" (strings/maybe-pr-str "a string"))))
    (testing "resturns pr-str'ed values"
      (are [v s] (= s (strings/maybe-pr-str v))
        {:a #{1}} "{:a #{1}}"
        ["a" \b 'c] #?(:clj "[\"a\" \\b c]" :cljs "[\"a\" \"b\" c]")))))

(deftest ^:unit titlize-test
  (testing "(titlize)"
    (testing "titlizes strings"
      (are [in out] (= (strings/titlize in) out)
        "something-is-rotten" "Something-Is-Rotten"
        "WEIRD-NEVER-BOTHERS ME" "Weird-Never-Bothers me"
        "--This--Is--Fine--" "--This--Is--Fine--"))))

(defn run-tests []
  (t/run-tests))
