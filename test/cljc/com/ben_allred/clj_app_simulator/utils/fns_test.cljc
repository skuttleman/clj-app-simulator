(ns com.ben-allred.clj-app-simulator.utils.fns-test
  (:require [clojure.test :refer [deftest testing is are]]
            [com.ben-allred.clj-app-simulator.utils.fns :as fns]))

(deftest ^:unit orf-test
  (testing "(orf)"
    (testing "returns first truthy value"
      (are [expected args] (= expected (apply fns/orf args))
        :thing [nil false :thing]
        :first [:first :second nil]
        true [false true false]))

    (testing "when there are no truthy values"
      (testing "returns nil"
        (are [expected args] (= expected (apply fns/orf args))
          nil [false false false]
          nil []
          nil [nil false])))))

(deftest ^:unit andf-test
  (testing "(andf)"
    (testing "returns last value before the first falsey value"
      (are [expected args] (= expected (apply fns/andf args))
        3 [1 2 3 nil]
        4 [1 2 3 4]
        nil [false true :true]
        nil [false 1 2 3 4 nil true false]
        nil []))))
