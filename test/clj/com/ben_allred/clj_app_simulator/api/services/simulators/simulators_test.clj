(ns com.ben-allred.clj-app-simulator.api.services.simulators.simulators-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.api.services.simulators.simulators :as sims]
            [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
            [test.utils.spies :as spies]))

(deftest ^:unit clear!-test
  (testing "(clear!)"
    (let [sims (atom {[::method-1 ::path-1] ::sim-1
                      [::method-1 ::path-2] ::sim-2
                      [::method-2 ::path-1] ::sim-3
                      [::method-2 ::path-2] ::sim-4})
          stop-spy (spies/create)]
      (with-redefs [sims/sims sims
                    common/stop stop-spy]
        (sims/clear!)
        (testing "stops all simulators"
          (is (spies/called-with? stop-spy ::sim-1))
          (is (spies/called-with? stop-spy ::sim-2))
          (is (spies/called-with? stop-spy ::sim-3))
          (is (spies/called-with? stop-spy ::sim-4)))

        (testing "removes all simulators"
          (is (empty? @sims)))))))

(deftest ^:unit add!-test
  (testing "(add!)"
    (let [sims (atom {[::method-1 ::path-1] ::sim-1
                      [::method-1 ::path-2] ::sim-2
                      [::method-2 ::path-1] ::sim-3
                      [::method-2 ::path-2] ::sim-4})
          start-spy (spies/create)
          identifier-spy (spies/constantly ::key)]
      (with-redefs [sims/sims sims
                    common/start start-spy
                    common/identifier identifier-spy]
        (testing "when the simulator does not exist"
          (let [result (sims/add! ::simulator)]
            (testing "gets the simulator's identifier"
              (is (spies/called-with? identifier-spy ::simulator)))

            (testing "adds the simulator"
              (is (= (get @sims ::key)
                     ::simulator)))

            (testing "starts the simulator"
              (is (spies/called-with? start-spy ::simulator)))

            (testing "returns the simulator"
              (is (= ::simulator result)))))

        (testing "when the simulator already exists"
          (spies/reset! start-spy)
          (reset! sims {::key ::ws})
          (let [result (sims/add! ::simulator)]
            (testing "does not add the simulator"
              (= (get @sims ::key)
                 ::ws))

            (testing "does not start the simulator"
              (spies/never-called? start-spy))

            (testing "returns nil"
              (is (nil? result)))))))))

(deftest ^:unit remove!-test
  (testing "(remove!)"
    (let [sims (atom {[::method ::path-1] ::sim-1
                      [::method ::path-2] ::sim-2})
          stop-spy (spies/create)]
      (with-redefs [sims/sims sims
                    common/stop stop-spy]
        (sims/remove! [::method ::path-2])
        (testing "when the simulator exists"
          (testing "stops the simulator"
            (is (spies/called-with? stop-spy ::sim-2))
            (is (spies/called-times? stop-spy 1)))

          (testing "removes the simulator"
            (is (nil? (get @sims [::method ::path-2])))
            (is (= (get @sims [::method ::path-1]) ::sim-1))))))))

(deftest ^:unit simulators-test
  (testing "(simulators)"
    (let [sims (atom {[::method ::path-1] ::sim-1
                      [::method ::path-2] ::sim-2})]
      (with-redefs [sims/sims sims]
        (testing "returns the simulators"
          (is (= #{::sim-1 ::sim-2}
                 (set (sims/simulators)))))))))
