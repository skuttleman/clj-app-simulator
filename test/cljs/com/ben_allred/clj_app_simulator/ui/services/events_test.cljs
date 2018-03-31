(ns ^:figwheel-load com.ben-allred.clj-app-simulator.ui.services.events-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [com.ben-allred.clj-app-simulator.ui.services.events :as events]))

(deftest ^:unit code->key-test
  (testing "(code->key)"
    (testing "translates 13"
      (is (= :enter (events/code->key 13))))
    (testing "translates 27"
      (is (= :esc (events/code->key 27))))
    (testing "return nil for unknown"
      (is (nil? (events/code->key 111111))))))

(deftest ^:unit key->code-test
  (testing "(key->code)"
    (testing "translates :enter"
      (is (= 13 (events/key->code :enter))))
    (testing "translates :esc"
      (is (= 27 (events/key->code :esc))))
    (testing "return nil for unknown"
      (is (nil? (events/key->code :not-a-key))))))

(deftest ^:unit ->key-code-test
  (testing "(->key-code)"
    (testing "handles enter"
      (is (= :enter (events/->key-code (clj->js {:keyCode 13})))))
    (testing "handles esc"
      (is (= :esc (events/->key-code (clj->js {:keyCode 27})))))
    (testing "handles ctrl + enter"
      (is (= :ctrl/enter (events/->key-code (clj->js {:keyCode 13
                                                      :ctrlKey true})))))
    (testing "handles meta + enter"
      (is (= :meta/enter (events/->key-code (clj->js {:keyCode 13
                                                      :metaKey true})))))
    (testing "handles shift + esc"
      (is (= :shift/esc (events/->key-code (clj->js {:keyCode  27
                                                     :shiftKey true})))))
    (testing "handles alt + esc"
      (is (= :alt/esc (events/->key-code (clj->js {:keyCode 27
                                                   :altKey  true})))))
    (testing "handles alt + shift + meta enter"
      (is (= :alt.meta.shift/enter
             (events/->key-code (clj->js {:keyCode  13
                                          :altKey   true
                                          :metaKey  true
                                          :shiftKey true})))))
    (testing "handles shift + meta + ctrl + alt + esc"
      (is (= :alt.ctrl.meta.shift/esc
             (events/->key-code (clj->js {:keyCode  27
                                          :altKey   true
                                          :metaKey  true
                                          :ctrlKey  true
                                          :shiftKey true})))))))
