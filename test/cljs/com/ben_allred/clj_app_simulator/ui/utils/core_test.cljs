(ns com.ben-allred.clj-app-simulator.ui.utils.core-test
    (:require [cljs.test :refer-macros [deftest testing is]]
              [com.ben-allred.clj-app-simulator.ui.utils.core :as utils]))

(deftest ^:unit classes-test
    (testing "(classes)"
        (testing "generates classes based on rules"
            (let [result (utils/classes {"class-1" true "class-2" false})]
                (is (= {:class-name "class-1"} result))))
        (testing "appends to existing class-name"
            (let [result (utils/classes {:class-name "existing-class"}
                                        {"class-1" true "class-2" false})]
                (is (= {:class-name "existing-class class-1"} result))))
        (testing "preserves other attributes"
            (let [result (utils/classes {::other ::attrs}
                                        {"class-1" true "class-2" false})]
                (is (= {::other ::attrs :class-name "class-1"}
                       result))))
        (testing "does not add :class-name when no classess are added"
            (let [result (utils/classes {::other ::attrs}
                                        {"class-1" false "class-2" false})]
                (is (= {::other ::attrs}
                       result))))))
