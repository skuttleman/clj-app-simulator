(ns com.ben-allred.clj-app-simulator.ui.services.store.middleware-test
  (:require [cljs.test :as t :refer-macros [deftest testing is]]
            [com.ben-allred.clj-app-simulator.ui.services.store.middleware :as mw]
            [test.utils.spies :as spies]))

(deftest ^:unit sims->sim-test
  (testing "(sims->sim)"
    (let [dispatch-spy (spies/create)
          mw ((mw/sims->sim ::get-state) dispatch-spy)]
      (testing "when type is :simulators.fetch-all/succeed"
        (spies/reset! dispatch-spy)
        (mw [:simulators.fetch-all/succeed {:simulators [::simulator-1 ::simulator-2]}])
        (testing "calls next with :simulators/clear first"
          (is (= [:simulators/clear] (ffirst (spies/calls dispatch-spy)))))
        (testing "calls next with :simulators.fetch-one/succeed"
          (is (spies/called-with? dispatch-spy [:simulators.fetch-one/succeed {:simulator ::simulator-1}]))
          (is (spies/called-with? dispatch-spy [:simulators.fetch-one/succeed {:simulator ::simulator-2}]))))
      (testing "when type is any random type"
        (spies/reset! dispatch-spy)
        (mw [:any-random-type ::value])
        (testing "passes action through to next"
          (is (spies/called-with? dispatch-spy [:any-random-type ::value])))))))

(defn run-tests [] (t/run-tests))
