(ns com.ben-allred.app-simulator.api.services.simulators.core-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [com.ben-allred.app-simulator.api.services.activity :as activity]
    [com.ben-allred.app-simulator.api.services.simulators.common :as common]
    [com.ben-allred.app-simulator.api.services.simulators.core :as simulators]
    [com.ben-allred.app-simulator.api.services.simulators.file :as file.sim]
    [com.ben-allred.app-simulator.api.services.simulators.http :as http.sim]
    [com.ben-allred.app-simulator.api.services.simulators.simulators :as sims]
    [com.ben-allred.app-simulator.api.services.simulators.ws :as ws.sim]
    [com.ben-allred.app-simulator.api.utils.respond :as respond]
    [com.ben-allred.app-simulator.utils.colls :as colls]
    [com.ben-allred.app-simulator.utils.maps :as maps]
    [compojure.core :as c]
    [integration.utils.http :as test.http]
    [test.utils.spies :as spies])
  (:import
    (clojure.lang ExceptionInfo)))

(deftest ^:unit valid?-test
  (testing "(valid?)"
    (with-redefs [http.sim/valid? (spies/create)
                  ws.sim/valid? (spies/create)
                  file.sim/valid? (spies/create)]
      (testing "when the http validator is successful"
        (spies/reset! http.sim/valid? ws.sim/valid? file.sim/valid?)
        (spies/respond-with! http.sim/valid? (constantly true))

        (testing "succeeds"
          (is (simulators/valid? ::config))))

      (testing "when the ws validator is successful"
        (spies/reset! http.sim/valid? ws.sim/valid? file.sim/valid?)
        (spies/respond-with! ws.sim/valid? (constantly true))

        (testing "succeeds"
          (is (simulators/valid? ::config))))

      (testing "when the file validator is successful"
        (spies/reset! http.sim/valid? ws.sim/valid? file.sim/valid?)
        (spies/respond-with! file.sim/valid? (constantly true))

        (testing "succeeds"
          (is (simulators/valid? ::config))))

      (testing "when no validators are successful"
        (spies/reset! http.sim/valid? ws.sim/valid? file.sim/valid?)

        (testing "fails"
          (is (not (simulators/valid? ::config))))))))

(deftest ^:unit config->?simulator-test
  (testing "(config->?simulator)"
    (with-redefs [http.sim/->HttpSimulator (spies/create)
                  ws.sim/->WsSimulator (spies/create)
                  file.sim/->FileSimulator (spies/create)]
      (testing "when the config can be used to build an http simulator"
        (spies/reset! http.sim/->HttpSimulator ws.sim/->WsSimulator file.sim/->FileSimulator)
        (spies/respond-with! http.sim/->HttpSimulator (constantly ::simulator))

        (testing "returns the simulator"
          (let [result (simulators/config->?simulator ::env ::config)]
            (is (spies/called-with? http.sim/->HttpSimulator ::env (spies/matcher uuid?) ::config))
            (is (= result ::simulator)))))

      (testing "when the config can be used to build a ws simulator"
        (spies/reset! http.sim/->HttpSimulator ws.sim/->WsSimulator file.sim/->FileSimulator)
        (spies/respond-with! ws.sim/->WsSimulator (constantly ::simulator))

        (testing "returns the simulator"
          (let [result (simulators/config->?simulator ::env ::config)]
            (is (spies/called-with? ws.sim/->WsSimulator ::env (spies/matcher uuid?) ::config))
            (is (= result ::simulator)))))

      (testing "when the config can be used to build a file simulator"
        (spies/reset! http.sim/->HttpSimulator ws.sim/->WsSimulator file.sim/->FileSimulator)
        (spies/respond-with! file.sim/->FileSimulator (constantly ::simulator))

        (testing "returns the simulator"
          (let [result (simulators/config->?simulator ::env ::config)]
            (is (spies/called-with? file.sim/->FileSimulator ::env (spies/matcher uuid?) ::config))
            (is (= result ::simulator)))))

      (testing "when the config cannot be used to build any simulator"
        (spies/reset! http.sim/->HttpSimulator ws.sim/->WsSimulator file.sim/->FileSimulator)

        (testing "returns nil"
          (is (nil? (simulators/config->?simulator ::env ::config))))))))

(deftest ^:unit make-simulator!-test
  (testing "(make-simulator!)"
    (with-redefs [simulators/config->?simulator (spies/constantly ::simulator)
                  sims/add! (spies/constantly ::added)]
      (testing "when a simulator is created"
        (spies/reset! simulators/config->?simulator sims/add!)
        (let [result (simulators/make-simulator! ::env ::config)]
          (testing "adds the simulator"
            (is (spies/called-with? simulators/config->?simulator ::env ::config))
            (is (spies/called-with? sims/add! ::env ::simulator)))

          (testing "returns the simulator"
            (is (= ::added result)))))

      (testing "when a simulator is not created"
        (spies/reset! simulators/config->?simulator sims/add!)
        (spies/respond-with! simulators/config->?simulator (constantly nil))
        (testing "throws an exception"
          (is (thrown? ExceptionInfo (simulators/make-simulator! ::env ::config))))))))

(deftest ^:unit details-test
  (testing "(details)"
    (with-redefs [sims/simulators (spies/constantly [::sim-1 ::sim-2])
                  common/details (spies/create (colls/onto [::details]))]
      (let [result (simulators/details ::env)]
        (testing "returns the simulators' details"
          (is (test.http/success? (respond/with result)))
          (is (= {:simulators [[::details ::sim-1]
                               [::details ::sim-2]]}
                 (second result)))
          (is (spies/called-with? sims/simulators ::env))
          (is (spies/called-with? common/details ::sim-1))
          (is (spies/called-with? common/details ::sim-2)))))))

(deftest ^:unit add-test
  (testing "(add)"
    (with-redefs [simulators/make-simulator! (spies/constantly ::simulator)
                  common/details (constantly ::details)
                  activity/publish (spies/create)
                  simulators/valid? (spies/constantly true)]
      (testing "makes a simulator"
        (spies/reset! simulators/make-simulator! activity/publish)
        (simulators/add ::env ::config)
        (is (spies/called-with? simulators/make-simulator! ::env ::config)))

      (testing "when a simulator is made"
        (spies/reset! simulators/make-simulator! activity/publish)
        (let [result (simulators/add ::env ::config)]
          (testing "publishes an event"
            (is (spies/called-with? activity/publish ::env :simulators/add {:simulator ::details})))

          (testing "returns a the simulator's details"
            (is (test.http/success? (respond/with result)))
            (is (= {:simulator ::details}
                   (second result))))))

      (testing "when the simulator is not valid"
        (spies/reset! activity/publish)
        (spies/respond-with! simulators/valid? (constantly false))
        (testing "throws an exception"
          (is (thrown? ExceptionInfo (simulators/add ::env ::config))))

        (testing "does not publish an event"
          (is (spies/never-called? activity/publish)))))))

(deftest ^:unit set!-test
  (testing "(set!)"
    (with-redefs [simulators/valid? (spies/constantly true)
                  sims/clear! (spies/create)
                  simulators/make-simulator! (spies/create (colls/onto [::simulator]))
                  common/details (spies/create (maps/onto :details))
                  activity/publish (spies/create)]
      (testing "when all configs are valid"
        (spies/reset! simulators/valid? sims/clear! simulators/make-simulator! common/details activity/publish)
        (let [result (simulators/set! ::env [::cfg-1 ::cfg-2 ::cfg-3])]
          (is (spies/called-with? simulators/valid? ::cfg-1))
          (is (spies/called-with? simulators/valid? ::cfg-2))
          (is (spies/called-with? simulators/valid? ::cfg-3))

          (testing "clears the simulators"
            (is (spies/called-with? sims/clear! ::env)))

          (testing "publishes an event"
            (is (spies/called-with? activity/publish
                                    ::env
                                    :simulators/init
                                    {:simulators [{:details [::simulator ::env ::cfg-1]}
                                                  {:details [::simulator ::env ::cfg-2]}
                                                  {:details [::simulator ::env ::cfg-3]}]})))

          (testing "returns the simulators' details"
            (is (test.http/success? (respond/with result)))
            (is (= {:simulators [{:details [::simulator ::env ::cfg-1]}
                                 {:details [::simulator ::env ::cfg-2]}
                                 {:details [::simulator ::env ::cfg-3]}]}
                   (second result))))

          (testing "makes the simulators"
            (is (spies/called-with? simulators/make-simulator! ::env ::cfg-1))
            (is (spies/called-with? simulators/make-simulator! ::env ::cfg-2))
            (is (spies/called-with? simulators/make-simulator! ::env ::cfg-3))
            (is (spies/called-with? common/details [::simulator ::env ::cfg-1]))
            (is (spies/called-with? common/details [::simulator ::env ::cfg-2]))
            (is (spies/called-with? common/details [::simulator ::env ::cfg-3])))))

      (testing "when not all configs are valid"
        (spies/reset! simulators/valid? sims/clear! simulators/make-simulator! common/details activity/publish)
        (spies/respond-with! simulators/valid? (constantly false))
        (testing "throws an exception"
          (is (thrown? ExceptionInfo (simulators/set! ::env [::config-1 ::config-2]))))

        (testing "does not publish an event"
          (is (spies/never-called? activity/publish)))))))

(deftest ^:unit reset-all!-test
  (testing "(reset-all!)"
    (with-redefs [sims/simulators (spies/constantly [::sim-1 ::sim-2])
                  common/reset! (spies/create)
                  activity/publish (spies/create)
                  common/details (spies/create (colls/onto [::details]))]
      (let [result (simulators/reset-all! ::env)]
        (testing "resets every sim"
          (is (spies/called-with? sims/simulators ::env))
          (is (spies/called-with? common/reset! ::sim-1))
          (is (spies/called-with? common/reset! ::sim-2)))

        (testing "publishes an event with details"
          (is (spies/called-with? activity/publish
                                  ::env
                                  :simulators/reset-all
                                  {:simulators [[::details ::sim-1]
                                                [::details ::sim-2]]}))
          (is (spies/called-with? common/details ::sim-1))
          (is (spies/called-with? common/details ::sim-2)))

        (testing "returns a success response"
          (is (test.http/success? (respond/with result))))))))

(deftest ^:unit routes-test
  (testing "(routes)"
    (with-redefs [sims/simulators (spies/constantly [::sim-1 ::sim-2])
                  common/routes (spies/constantly [::route-1 ::route-2])
                  c/routes (spies/constantly ::route)]
      (let [result (simulators/routes ::env)]
        (testing "gets route data for each simulator"
          (is (spies/called-with? sims/simulators ::env))
          (is (spies/called-with? common/routes ::sim-1))
          (is (spies/called-with? common/routes ::sim-2)))

        (testing "makes routes"
          (is (spies/called-with? c/routes ::route-1 ::route-2 ::route-1 ::route-2)))

        (testing "returns composed route"
          (is (= ::route result)))))))
