(ns com.ben-allred.app-simulator.api.services.simulators.simulators-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [com.ben-allred.app-simulator.api.services.simulators.common :as common]
    [com.ben-allred.app-simulator.api.services.simulators.simulators :as sims]
    [test.utils.spies :as spies])
  (:import
    (clojure.lang MapEntry)))

(deftest ^:unit clear!-test
  (testing "(clear!)"
    (let [sims (atom {::env         #{::sim-1 ::sim-2 ::sim-3 ::sim-4}
                      ::another-env #{::sim-5}})]
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
          (is (= (::another-env @sims) #{::sim-5}))
          (is (empty? (::env @sims))))))))

(deftest ^:unit add!-test
  (testing "(add!)"
    (let [sims (atom {::env #{::sim-1 ::sim-2 ::sim-3 ::sim-4}})]
      (with-redefs [sims/sims sims
                    common/start! (spies/create)]
        (testing "when the simulator does not exist"
          (let [result (sims/add! ::env ::simulator)]
            (testing "adds the simulator"
              (is (contains? (get @sims ::env) ::simulator)))

            (testing "starts the simulator"
              (is (spies/called-with? common/start! ::simulator)))

            (testing "returns the simulator"
              (is (= ::simulator result)))))

        (testing "when the simulator already exists"
          (spies/reset! common/start!)
          (reset! sims {::env #{::simulator}})
          (let [result (sims/add! ::env ::simulator)]
            (testing "does not start the simulator"
              (spies/never-called? common/start!))

            (testing "returns nil"
              (is (nil? result)))))))))

(deftest ^:unit remove!-test
  (testing "(remove!)"
    (let [sims (atom {::env #{::sim-1 ::sim-2}})]
      (with-redefs [sims/sims sims
                    common/stop! (spies/create)]
        (sims/remove! ::env ::sim-2)
        (testing "when the simulator exists"
          (testing "stops the simulator"
            (is (spies/called-with? common/stop! ::sim-2))
            (is (spies/called-times? common/stop! 1)))

          (testing "removes the simulator"
            (is (= #{::sim-1} (::env @sims)))))))))

(deftest ^:unit simulators-test
  (testing "(simulators)"
    (let [sims (atom {::env #{::sim-1 ::sim-2}})]
      (with-redefs [sims/sims sims]
        (testing "returns the simulators"
          (is (= #{::sim-1 ::sim-2}
                 (set (sims/simulators ::env)))))))))

(deftest ^:unit get-test
  (testing "(get)"
    (with-redefs [sims/sims (atom {::env [::sim-1 ::sim-2 ::sim-3 ::sim-4]})
                  common/details (spies/create)]
      (testing "finds a simulator by its id"
        (spies/returning! common/details {:id 123} {:id 456} {:id 789} {:id 000})
        (is (= ::sim-3 (sims/get ::env 789))))

      (testing "returns nil if no simulator is found"
        (spies/reset! common/details)
        (is (nil? (sims/get ::env 999)))))))
