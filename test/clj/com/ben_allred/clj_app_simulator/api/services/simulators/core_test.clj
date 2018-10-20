(ns com.ben-allred.clj-app-simulator.api.services.simulators.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
            [com.ben-allred.clj-app-simulator.api.services.simulators.core :as simulators]
            [com.ben-allred.clj-app-simulator.api.services.simulators.http :as http.sim]
            [com.ben-allred.clj-app-simulator.api.services.simulators.ws :as ws.sim]
            [com.ben-allred.clj-app-simulator.api.services.activity :as activity]
            [com.ben-allred.clj-app-simulator.api.utils.respond :as respond]
            [com.ben-allred.clj-app-simulator.api.services.simulators.simulators :as sims]
            [com.ben-allred.clj-app-simulator.api.services.simulators.file :as file.sim]
            [compojure.core :as c]
            [integration.utils.http :as test.http]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.utils.colls :as colls]
            [com.ben-allred.clj-app-simulator.utils.maps :as maps]))

(deftest ^:unit valid?-test
  (testing "(valid?)"
    (let [http-spy (spies/create)
          ws-spy (spies/create)
          file-spy (spies/create)]
      (with-redefs [http.sim/valid? http-spy
                    ws.sim/valid? ws-spy
                    file.sim/valid? file-spy]
        (testing "when the http validator is successful"
          (spies/reset! http-spy ws-spy file-spy)
          (spies/respond-with! http-spy (constantly true))

          (testing "succeeds"
            (is (simulators/valid? ::config))))

        (testing "when the ws validator is successful"
          (spies/reset! http-spy ws-spy file-spy)
          (spies/respond-with! ws-spy (constantly true))

          (testing "succeeds"
            (is (simulators/valid? ::config))))

        (testing "when the file validator is successful"
          (spies/reset! http-spy ws-spy file-spy)
          (spies/respond-with! file-spy (constantly true))

          (testing "succeeds"
            (is (simulators/valid? ::config))))

        (testing "when no validators are successful"
          (spies/reset! http-spy ws-spy file-spy)

          (testing "fails"
            (is (not (simulators/valid? ::config)))))))))

(deftest ^:unit config->?simulator-test
  (testing "(config->?simulator)"
    (let [http-spy (spies/create)
          ws-spy (spies/create)
          file-spy (spies/create)]
      (with-redefs [http.sim/->HttpSimulator http-spy
                    ws.sim/->WsSimulator ws-spy
                    file.sim/->FileSimulator file-spy]
        (testing "when the config can be used to build an http simulator"
          (spies/reset! http-spy ws-spy file-spy)
          (spies/respond-with! http-spy (constantly ::simulator))

          (testing "returns the simulator"
            (let [result (simulators/config->?simulator ::env ::config)]
              (is (spies/called-with? http-spy ::env (spies/matcher uuid?) ::config))
              (is (= result ::simulator)))))

        (testing "when the config can be used to build a ws simulator"
          (spies/reset! http-spy ws-spy file-spy)
          (spies/respond-with! ws-spy (constantly ::simulator))

          (testing "returns the simulator"
            (let [result (simulators/config->?simulator ::env ::config)]
              (is (spies/called-with? ws-spy ::env (spies/matcher uuid?) ::config))
              (is (= result ::simulator)))))

        (testing "when the config can be used to build a file simulator"
          (spies/reset! http-spy ws-spy file-spy)
          (spies/respond-with! file-spy (constantly ::simulator))

          (testing "returns the simulator"
            (let [result (simulators/config->?simulator ::env ::config)]
              (is (spies/called-with? file-spy ::env (spies/matcher uuid?) ::config))
              (is (= result ::simulator)))))

        (testing "when the config cannot be used to build any simulator"
          (spies/reset! http-spy ws-spy file-spy)

          (testing "returns nil"
            (is (nil? (simulators/config->?simulator ::env ::config)))))))))

(deftest ^:unit make-simulator!-test
  (testing "(make-simulator!)"
    (let [config-spy (spies/constantly ::simulator)
          add-spy (spies/constantly ::added)]
      (with-redefs [simulators/config->?simulator config-spy
                    sims/add! add-spy]
        (testing "when a simulator is created"
          (spies/reset! config-spy add-spy)
          (let [result (simulators/make-simulator! ::env ::config)]
            (testing "adds the simulator"
              (is (spies/called-with? config-spy ::env ::config))
              (is (spies/called-with? add-spy ::env ::simulator)))

            (testing "returns the simulator"
              (is (= ::added result)))))

        (testing "when a simulator is not created"
          (spies/reset! config-spy add-spy)
          (spies/respond-with! config-spy (constantly nil))
          (let [result (simulators/make-simulator! ::env ::config)]
            (testing "does not add the simulator"
              (is (spies/never-called? add-spy)))

            (testing "returns nil"
              (is (nil? result)))))))))

(deftest ^:unit details-test
  (testing "(details)"
    (let [simulators-spy (spies/constantly [::sim-1 ::sim-2])
          details-spy (spies/create (colls/onto [::details]))]
      (with-redefs [sims/simulators simulators-spy
                    common/details details-spy]
        (let [result (simulators/details ::env)]
          (testing "returns the simulators' details"
            (is (test.http/success? (respond/with result)))
            (is (= {:simulators [[::details ::sim-1]
                                 [::details ::sim-2]]}
                   (second result)))
            (is (spies/called-with? simulators-spy ::env))
            (is (spies/called-with? details-spy ::sim-1))
            (is (spies/called-with? details-spy ::sim-2))))))))

(deftest ^:unit add-test
  (testing "(add)"
    (let [make-spy (spies/constantly ::simulator)
          details-spy (spies/constantly ::details)
          publish-spy (spies/create)]
      (with-redefs [simulators/make-simulator! make-spy
                    common/details details-spy
                    activity/publish publish-spy]
        (testing "makes a simulator"
          (spies/reset! make-spy details-spy publish-spy)
          (simulators/add ::env ::config)
          (is (spies/called-with? make-spy ::env ::config)))

        (testing "when a simulator is made"
          (spies/reset! make-spy details-spy publish-spy)
          (let [result (simulators/add ::env ::config)]
            (testing "publishes an event"
              (is (spies/called-with? publish-spy ::env :simulators/add ::details)))

            (testing "returns a the simulator's details"
              (is (test.http/success? (respond/with result)))
              (is (= {:simulator ::details}
                     (second result))))))

        (testing "when a simulator is not made"
          (spies/reset! make-spy details-spy publish-spy)
          (spies/respond-with! make-spy (constantly nil))
          (let [result (simulators/add ::env ::config)]
            (testing "does not publish an event"
              (is (spies/never-called? publish-spy)))

            (testing "returns an error"
              (is (test.http/client-error? (respond/with result))))))))))

(deftest ^:unit set!-test
  (testing "(set!)"
    (let [valid-spy (spies/constantly true)
          clear-spy (spies/create)
          make-spy (spies/create (colls/onto [::simulator]))
          details-spy (spies/create (maps/onto :details))
          publish-spy (spies/create)]
      (with-redefs [simulators/valid? valid-spy
                    sims/clear! clear-spy
                    simulators/make-simulator! make-spy
                    common/details details-spy
                    activity/publish publish-spy]
        (testing "when all configs are valid"
          (spies/reset! valid-spy clear-spy make-spy details-spy publish-spy)
          (let [result (simulators/set! ::env [::cfg-1 ::cfg-2 ::cfg-3])]
            (is (spies/called-with? valid-spy ::cfg-1))
            (is (spies/called-with? valid-spy ::cfg-2))
            (is (spies/called-with? valid-spy ::cfg-3))

            (testing "clears the simulators"
              (is (spies/called-with? clear-spy ::env)))

            (testing "publishes an event"
              (is (spies/called-with? publish-spy
                                      ::env
                                      :simulators/init
                                      [{:details [::simulator ::env ::cfg-1]}
                                       {:details [::simulator ::env ::cfg-2]}
                                       {:details [::simulator ::env ::cfg-3]}])))

            (testing "returns the simulators' details"
              (is (test.http/success? (respond/with result)))
              (is (= {:simulators [{:details [::simulator ::env ::cfg-1]}
                                   {:details [::simulator ::env ::cfg-2]}
                                   {:details [::simulator ::env ::cfg-3]}]}
                     (second result))))

            (testing "makes the simulators"
              (is (spies/called-with? make-spy ::env ::cfg-1))
              (is (spies/called-with? make-spy ::env ::cfg-2))
              (is (spies/called-with? make-spy ::env ::cfg-3))
              (is (spies/called-with? details-spy [::simulator ::env ::cfg-1]))
              (is (spies/called-with? details-spy [::simulator ::env ::cfg-2]))
              (is (spies/called-with? details-spy [::simulator ::env ::cfg-3])))))

        (testing "when not all configs are valid"
          (spies/reset! valid-spy clear-spy make-spy details-spy publish-spy)
          (spies/respond-with! valid-spy (constantly false))
          (let [result (simulators/set! ::env [::config-1 ::config-2])]
            (testing "does not clear the simulators"
              (is (spies/never-called? clear-spy)))

            (testing "does not publish an event"
              (is (spies/never-called? publish-spy)))

            (testing "returns an error"
              (is (test.http/client-error? (respond/with result))))))))))

(deftest ^:unit reset-all!-test
  (testing "(reset-all!)"
    (let [simulators-spy (spies/constantly [::sim-1 ::sim-2])
          reset-spy (spies/create)
          publish-spy (spies/create)
          details-spy (spies/create (colls/onto [::details]))]
      (with-redefs [sims/simulators simulators-spy
                    common/reset! reset-spy
                    activity/publish publish-spy
                    common/details details-spy]
        (let [result (simulators/reset-all! ::env)]
          (testing "resets every sim"
            (is (spies/called-with? simulators-spy ::env))
            (is (spies/called-with? reset-spy ::sim-1))
            (is (spies/called-with? reset-spy ::sim-2)))

          (testing "publishes an event with details"
            (is (spies/called-with? publish-spy
                                    ::env
                                    :simulators/reset-all
                                    [[::details ::sim-1]
                                     [::details ::sim-2]]))
            (is (spies/called-with? details-spy ::sim-1))
            (is (spies/called-with? details-spy ::sim-2)))

          (testing "returns a success response"
            (is (test.http/success? (respond/with result)))))))))

(deftest ^:unit routes-test
  (testing "(routes)"
    (let [simulators-spy (spies/constantly [::sim-1 ::sim-2])
          routes-spy (spies/constantly [::route-1 ::route-2])
          make-routes-spy (spies/constantly ::route)]
      (with-redefs [sims/simulators simulators-spy
                    common/routes routes-spy
                    c/routes make-routes-spy]
        (let [result (simulators/routes ::env)]
          (testing "gets route data for each simulator"
            (is (spies/called-with? simulators-spy ::env))
            (is (spies/called-with? routes-spy ::sim-1))
            (is (spies/called-with? routes-spy ::sim-2)))

          (testing "makes routes"
            (is (spies/called-with? make-routes-spy ::route-1 ::route-2 ::route-1 ::route-2)))

          (testing "returns composed route"
            (is (= ::route result))))))))
