(ns com.ben-allred.app-simulator.ui.utils.macros-test
  (:require
    [clojure.test :as t :refer-macros [deftest is testing]]
    [com.ben-allred.app-simulator.ui.utils.macros :as macros :include-macros true]
    [test.utils.spies :as spies]))

(deftest ^:unit after-test
  (testing "(after)"
    (testing "sets timeout with fn body"
      (with-redefs [macros/set-timeout (spies/create)]
        (let [some-spy (spies/create)
              some-other-spy (spies/create)]
          (macros/after 100 (some-spy ::some) (some-other-spy ::other) (+ 1 2))
          (is (spies/never-called? some-spy))
          (is (spies/never-called? some-other-spy))
          (let [f (ffirst (spies/calls macros/set-timeout))
                result (f)]
            (is (spies/called-with? some-spy ::some))
            (is (spies/called-with? some-other-spy ::other))
            (is (= 3 result))))))))

(defn run-tests []
  (t/run-tests))
