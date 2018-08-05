(ns com.ben-allred.clj-app-simulator.api.services.simulators.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
            [com.ben-allred.clj-app-simulator.api.services.simulators.core :as simulators]
            [com.ben-allred.clj-app-simulator.api.services.simulators.http :as http.sim]
            [com.ben-allred.clj-app-simulator.api.services.simulators.ws :as ws.sim]
            [com.ben-allred.clj-app-simulator.api.services.activity :as activity]
            [compojure.core :as c]
            [com.ben-allred.clj-app-simulator.api.services.simulators.simulators :as sims]
            [integration.utils.http :as test.http]
            [com.ben-allred.clj-app-simulator.api.services.simulators.file :as file.sim]))

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
            (let [result (simulators/config->?simulator ::config)]
              (is (spies/called-with? http-spy (spies/matcher uuid?) ::config))
              (is (= result ::simulator)))))

        (testing "when the config can be used to build a ws simulator"
          (spies/reset! http-spy ws-spy file-spy)
          (spies/respond-with! ws-spy (constantly ::simulator))

          (testing "returns the simulator"
            (let [result (simulators/config->?simulator ::config)]
              (is (spies/called-with? ws-spy (spies/matcher uuid?) ::config))
              (is (= result ::simulator)))))

        (testing "when the config can be used to build a file simulator"
          (spies/reset! http-spy ws-spy file-spy)
          (spies/respond-with! file-spy (constantly ::simulator))

          (testing "returns the simulator"
            (let [result (simulators/config->?simulator ::config)]
              (is (spies/called-with? file-spy (spies/matcher uuid?) ::config))
              (is (= result ::simulator)))))

        (testing "when the config cannot be used to build any simulator"
          (spies/reset! http-spy ws-spy file-spy)

          (testing "returns nil"
            (is (nil? (simulators/config->?simulator ::config)))))))))

(deftest ^:unit make-simulator!-test
  (testing "(make-simulator!)"
    (let [config-spy (spies/constantly ::simulator)
          add-spy (spies/constantly ::added)]
      (with-redefs [simulators/config->?simulator config-spy
                    sims/add! add-spy]
        (testing "when a simulator is created"
          (spies/reset! config-spy add-spy)
          (let [result (simulators/make-simulator! ::config)]
            (testing "adds the simulator"
              (is (spies/called-with? config-spy ::config))
              (is (spies/called-with? add-spy ::simulator)))

            (testing "returns the simulator"
              (is (= ::added result)))))

        (testing "when a simulator is not created"
          (spies/reset! config-spy add-spy)
          (spies/respond-with! config-spy (constantly nil))
          (let [result (simulators/make-simulator! ::config)]
            (testing "does not add the simulator"
              (is (spies/never-called? add-spy)))

            (testing "returns nil"
              (is (nil? result)))))))))

(deftest ^:unit details-test
  (testing "(details)"
    (let [simulators-spy (spies/constantly [::sim-1 ::sim-2])
          details-spy (spies/create (partial conj [::details]))]
      (with-redefs [sims/simulators simulators-spy
                    common/details details-spy]
        (let [result (simulators/details)]
          (testing "returns the simulators' details"
            (is (test.http/success? result))
            (is (= {:simulators [[::details ::sim-1]
                                 [::details ::sim-2]]}
                   (:body result)))
            (is (spies/called-with? simulators-spy))
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
          (simulators/add ::config)
          (is (spies/called-with? make-spy ::config)))

        (testing "when a simulator is made"
          (spies/reset! make-spy details-spy publish-spy)
          (let [result (simulators/add ::config)]
            (testing "publishes an event"
              (is (spies/called-with? publish-spy :simulators/add ::details)))

            (testing "returns a the simulator's details"
              (is (test.http/success? result))
              (is (= {:simulator ::details}
                     (:body result))))))

        (testing "when a simulator is not made"
          (spies/reset! make-spy details-spy publish-spy)
          (spies/respond-with! make-spy (constantly nil))
          (let [result (simulators/add ::config)]
            (testing "does not publish an event"
              (is (spies/never-called? publish-spy)))

            (testing "returns an error"
              (is (test.http/client-error? result)))))))))

(deftest ^:unit set!-test
  (testing "(set!)"
    (let [valid-spy (spies/constantly true)
          clear-spy (spies/create)
          make-spy (spies/create (partial conj [::simulator]))
          details-spy (spies/create (partial assoc {} :details))
          publish-spy (spies/create)]
      (with-redefs [simulators/valid? valid-spy
                    sims/clear! clear-spy
                    simulators/make-simulator! make-spy
                    common/details details-spy
                    activity/publish publish-spy]
        (testing "when all configs are valid"
          (spies/reset! valid-spy clear-spy make-spy details-spy publish-spy)
          (let [result (simulators/set! [::cfg-1 ::cfg-2 ::cfg-3])]
            (is (spies/called-with? valid-spy ::cfg-1))
            (is (spies/called-with? valid-spy ::cfg-2))
            (is (spies/called-with? valid-spy ::cfg-3))

            (testing "clears the simulators"
              (is (spies/called? clear-spy)))

            (testing "publishes an event"
              (is (spies/called-with? publish-spy
                                      :simulators/init
                                      [{:details [::simulator ::cfg-1]}
                                       {:details [::simulator ::cfg-2]}
                                       {:details [::simulator ::cfg-3]}])))

            (testing "returns the simulators' details"
              (is (test.http/success? result))
              (is (= {:simulators [{:details [::simulator ::cfg-1]}
                                   {:details [::simulator ::cfg-2]}
                                   {:details [::simulator ::cfg-3]}]}
                     (:body result))))

            (testing "makes the simulators"
              (is (spies/called-with? make-spy ::cfg-1))
              (is (spies/called-with? make-spy ::cfg-2))
              (is (spies/called-with? make-spy ::cfg-3))
              (is (spies/called-with? details-spy [::simulator ::cfg-1]))
              (is (spies/called-with? details-spy [::simulator ::cfg-2]))
              (is (spies/called-with? details-spy [::simulator ::cfg-3])))))

        (testing "when not all configs are valid"
          (spies/reset! valid-spy clear-spy make-spy details-spy publish-spy)
          (spies/respond-with! valid-spy (constantly false))
          (let [result (simulators/set! [::config-1 ::config-2])]
            (testing "does not clear the simulators"
              (is (spies/never-called? clear-spy)))

            (testing "does not publish an event"
              (is (spies/never-called? publish-spy)))

            (testing "returns an error"
              (is (test.http/client-error? result)))))))))

(deftest ^:unit reset-all!-test
  (testing "(reset-all!)"
    (let [simulators-spy (spies/constantly [::sim-1 ::sim-2])
          reset-spy (spies/create)
          publish-spy (spies/create)
          details-spy (spies/create (partial conj [::details]))]
      (with-redefs [sims/simulators simulators-spy
                    common/reset reset-spy
                    activity/publish publish-spy
                    common/details details-spy]
        (let [result (simulators/reset-all!)]
          (testing "resets every sim"
            (is (spies/called-with? simulators-spy))
            (is (spies/called-with? reset-spy ::sim-1))
            (is (spies/called-with? reset-spy ::sim-2)))

          (testing "publishes an event with details"
            (is (spies/called-with? publish-spy
                                    :simulators/reset-all
                                    [[::details ::sim-1]
                                     [::details ::sim-2]]))
            (is (spies/called-with? details-spy ::sim-1))
            (is (spies/called-with? details-spy ::sim-2)))

          (testing "returns a success response"
            (is (test.http/success? result))))))))

(deftest ^:unit routes-test
  (testing "(routes)"
    (let [simulators-spy (spies/constantly [::sim-1 ::sim-2])
          routes-spy (spies/constantly [::route-1 ::route-2])
          make-routes-spy (spies/constantly ::route)]
      (with-redefs [sims/simulators simulators-spy
                    common/routes routes-spy
                    c/routes make-routes-spy]
        (let [result (simulators/routes)]
          (testing "gets route data for each simulator"
            (is (spies/called-with? simulators-spy))
            (is (spies/called-with? routes-spy ::sim-1))
            (is (spies/called-with? routes-spy ::sim-2)))

          (testing "makes routes"
            (is (spies/called-with? make-routes-spy ::route-1 ::route-2 ::route-1 ::route-2)))

          (testing "returns composed route"
            (is (= ::route result))))))))
