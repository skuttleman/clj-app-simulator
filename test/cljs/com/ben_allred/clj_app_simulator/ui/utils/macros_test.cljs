(ns com.ben-allred.clj-app-simulator.ui.utils.macros-test
  (:require [clojure.test :as t :refer-macros [deftest testing is]]
            [com.ben-allred.clj-app-simulator.ui.utils.macros :as macros :include-macros true]
            [test.utils.spies :as spies]))

(deftest ^:unit after-test
  (testing "(after)"
    (let [timeout-spy (spies/create)
          some-spy (spies/create)
          some-other-spy (spies/create)]
      (testing "sets timeout with fn body"
        (with-redefs [macros/set-timeout timeout-spy]
          (macros/after 100 (some-spy ::some) (some-other-spy ::other) (+ 1 2))
          (is (spies/never-called? some-spy))
          (is (spies/never-called? some-other-spy))
          (let [f (ffirst (spies/calls timeout-spy))
                result (f)]
            (is (spies/called-with? some-spy ::some))
            (is (spies/called-with? some-other-spy ::other))
            (is (= 3 result))))))))

(defn run-tests []
  (t/run-tests))
