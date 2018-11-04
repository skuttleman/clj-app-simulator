(ns com.ben-allred.app-simulator.api.services.simulators.simulators-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [com.ben-allred.app-simulator.api.services.simulators.common :as common]
    [com.ben-allred.app-simulator.api.services.simulators.simulators :as sims]
    [test.utils.spies :as spies]))

(deftest ^:unit clear!-test
  (testing "(clear!)"
    (let [sims (atom {::env         {[::method-1 ::path-1] ::sim-1
                                     [::method-1 ::path-2] ::sim-2
                                     [::method-2 ::path-1] ::sim-3
                                     [::method-2 ::path-2] ::sim-4}
                      ::another-env {[::method-3 ::path-3] ::sim-5}})]
      (with-redefs [sims/sims sims
                    common/stop! (spies/create)]
        (sims/clear! ::env)
        (testing "stops all simulators"
          (is (spies/called-with? common/stop! ::sim-1))
          (is (spies/called-with? common/stop! ::sim-2))
          (is (spies/called-with? common/stop! ::sim-3))
          (is (spies/called-with? common/stop! ::sim-4))
          (is (not (spies/called-with? common/stop! ::sim-5))))

        (testing "removes all simulators for env"
          (is (= (::another-env @sims) {[::method-3 ::path-3] ::sim-5}))
          (is (empty? (::env @sims))))))))

(deftest ^:unit add!-test
  (testing "(add!)"
    (let [sims (atom {[::method-1 ::path-1] ::sim-1
                      [::method-1 ::path-2] ::sim-2
                      [::method-2 ::path-1] ::sim-3
                      [::method-2 ::path-2] ::sim-4})]
      (with-redefs [sims/sims sims
                    common/start! (spies/create)
                    common/identifier (spies/constantly ::key)]
        (testing "when the simulator does not exist"
          (let [result (sims/add! ::env ::simulator)]
            (testing "gets the simulator's identifier"
              (is (spies/called-with? common/identifier ::simulator)))

            (testing "adds the simulator"
              (is (= (get-in @sims [::env ::key]) ::simulator)))

            (testing "starts the simulator"
              (is (spies/called-with? common/start! ::simulator)))

            (testing "returns the simulator"
              (is (= ::simulator result)))))

        (testing "when the simulator already exists"
          (spies/reset! common/start!)
          (reset! sims {::env {::key ::ws}})
          (let [result (sims/add! ::env ::simulator)]
            (testing "does not add the simulator"
              (= ::ws (get-in @sims [::env ::key])))

            (testing "does not start the simulator"
              (spies/never-called? common/start!))

            (testing "returns nil"
              (is (nil? result)))))))))

(deftest ^:unit remove!-test
  (testing "(remove!)"
    (let [sims (atom {::env {[::method ::path-1] ::sim-1
                             [::method ::path-2] ::sim-2}})]
      (with-redefs [sims/sims sims
                    common/stop! (spies/create)]
        (sims/remove! ::env [::method ::path-2])
        (testing "when the simulator exists"
          (testing "stops the simulator"
            (is (spies/called-with? common/stop! ::sim-2))
            (is (spies/called-times? common/stop! 1)))

          (testing "removes the simulator"
            (is (nil? (get-in @sims [::env [::method ::path-2]])))
            (is (= (get-in @sims [::env [::method ::path-1]]) ::sim-1))))))))

(deftest ^:unit simulators-test
  (testing "(simulators)"
    (let [sims (atom {::env {[::method ::path-1] ::sim-1
                             [::method ::path-2] ::sim-2}})]
      (with-redefs [sims/sims sims]
        (testing "returns the simulators"
          (is (= #{::sim-1 ::sim-2}
                 (set (sims/simulators ::env)))))))))
