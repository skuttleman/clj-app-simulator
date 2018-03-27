(ns com.ben-allred.clj-app-simulator.api.services.simulators.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
            [com.ben-allred.clj-app-simulator.api.services.simulators.core :as simulators]
            [com.ben-allred.clj-app-simulator.api.services.simulators.http :as http.sim]
            [com.ben-allred.clj-app-simulator.api.services.activity :as activity]
            [compojure.core :as c]
            [com.ben-allred.clj-app-simulator.services.http :as http]))

(deftest ^:unit route-configs-test
  (testing "(configs)"
    (let [simulators (atom {:sim-1 (reify common/ISimulator
                                     (config [_]
                                       {::simulator 1}))
                            :sim-2 (reify common/ISimulator
                                     (config [_]
                                       {::simulator 2}))})]
      (with-redefs [simulators/simulators simulators]
        (testing "gets simulators' configs"
          (let [result (simulators/configs)]
            (is (= [{::simulator 1} {::simulator 2}] (get-in result [:body :simulators])))))
        (testing "returns response map"
          (let [result (simulators/configs)]
            (is (http/success? result))))))))

(deftest ^:unit add-simulator-test
  (testing "(add)"
    (let [simulator-atom (atom {})
          start-sim (spies/create (constantly ::started))
          simulator (reify common/ISimulator
                      (start [this]
                        (start-sim this))
                      (config [_]
                        {::some ::config}))
          http-sim-spy (spies/create (constantly simulator))
          http-why-spy (spies/create (constantly ::reason))
          publish-spy (spies/create)]
      (with-redefs [simulators/simulators simulator-atom
                    http.sim/->HttpSimulator http-sim-spy
                    activity/publish publish-spy]
        (testing "when a simulator can be added"
          (reset! simulator-atom {})
          (spies/reset! start-sim publish-spy)
          (let [result (simulators/add {:method "some/method"
                                        :path   ::path})]
            (testing "creates a simulator with keywordized method"
              (is (spies/called-with? http-sim-spy {:method :some/method
                                                    :path   ::path})))
            (testing "starts the simulator"
              (is (spies/called-with? start-sim simulator)))
            (testing "returns a success map"
              (is (http/success? result)))
            (testing "publishes an event"
              (is (spies/called-with? publish-spy :simulators/add {::some ::config}))))))
      (with-redefs [simulators/simulators simulator-atom
                    http.sim/->HttpSimulator http-sim-spy
                    activity/publish publish-spy
                    http.sim/why-not? http-why-spy]
        (testing "when a simulator cannot be added"
          (reset! simulator-atom {[::method ::path] ::simulator})
          (spies/reset! start-sim publish-spy)
          (spies/do-when-called-with! http-sim-spy
                                      #(= {:method ::method :path ::path} (first %))
                                      (constantly nil))
          (let [config {:method ::method :path ::path}
                result (simulators/add config)]
            (testing "does not overwrite existing simulator"
              (is (= ::simulator (get @simulator-atom [::method ::path]))))
            (testing "returns an error map"
              (is (http/client-error? result))
              (is (spies/called-with? http-why-spy config))
              (is (= ::reason (get-in result [:body :message]))))))))))

(deftest ^:unit set-simulators-test
  (testing "(set!)"
    (let [simulator-atom (atom {})
          http-valid-spy (spies/create (constantly true))
          http-why-spy (spies/create (constantly ::reason))
          start-sim (spies/create (constantly ::started))
          simulator-1 (reify common/ISimulator
                        (start [this]
                          (start-sim this))
                        (config [_]
                          {::config 1}))
          simulator-2 (reify common/ISimulator
                        (start [this]
                          (start-sim this))
                        (config [_]
                          {::config 2}))
          http-sim-spy (spies/and-then simulator-1 simulator-2)
          publisher-spy (spies/create)]
      (with-redefs [simulators/simulators simulator-atom
                    http.sim/valid? http-valid-spy
                    http.sim/->HttpSimulator http-sim-spy
                    activity/publish publisher-spy]
        (testing "when all configs are valid"
          (reset! simulator-atom {[::old-method ::old-path] ::old-simulator})
          (let [result (simulators/set! [{:method ::method :path ::path-1 ::config ::1}
                                         {:method ::method :path ::path-2 ::config ::2}])]
            (is (spies/called-with? http-valid-spy {:method ::method :path ::path-1 ::config ::1}))
            (is (spies/called-with? http-valid-spy {:method ::method :path ::path-2 ::config ::2}))
            (testing "sets multiple configs"
              (is (= simulator-1 (get @simulator-atom [::method ::path-1])))
              (is (= simulator-2 (get @simulator-atom [::method ::path-2]))))
            (testing "removes existing configs"
              (is (not (contains? @simulator-atom [::old-method ::old-path]))))
            (testing "publishes an event"
              (is (spies/called-with? publisher-spy :simulators/init [{::config 1} {::config 2}])))
            (testing "returns a success map"
              (is (http/success? result))))))
      (with-redefs [http.sim/valid? (spies/and-then true false)
                    simulators/simulators simulator-atom
                    http.sim/why-not? http-why-spy]
        (testing "when one more more configs are invalid"
          (reset! simulator-atom {[::old-method ::old-path] ::old-simulator})
          (let [result (simulators/set! [{:method ::method :path ::path-1 ::config ::1}
                                         {:method ::method :path ::path-2 ::config ::2}])
                {:keys [reason config]} (first (get-in result [:body :simulators]))]
            (testing "does not change current configs"
              (is (= @simulator-atom {[::old-method ::old-path] ::old-simulator})))
            (testing "returns an error map"
              (is (http/client-error? result))
              (is (= {:method ::method :path ::path-2 ::config ::2} config))
              (is (= ::reason reason)))))))))

(deftest ^:unit reset-all-test
  (testing "(reset-all!)"
    (let [reset-sim (spies/create)
          simulator-1 (reify common/ISimulator
                        (reset [this]
                          (reset-sim this))
                        (config [_]
                          ::config-1))
          simulator-2 (reify common/ISimulator
                        (reset [this]
                          (reset-sim this))
                        (config [_]
                          ::config-2))
          simulator-atom (atom {::key-1 simulator-1
                                ::key-2 simulator-2})
          publish-spy (spies/create)]
      (with-redefs [simulators/simulators simulator-atom
                    activity/publish publish-spy]
        (let [result (simulators/reset-all!)]
          (testing "resets all simulators"
            (is (spies/called-with? reset-sim simulator-1))
            (is (spies/called-with? reset-sim simulator-2)))
          (testing "publishes an event"
            (let [sims (last (first (spies/calls publish-spy)))]
              (is (spies/called-with? publish-spy :simulators/reset-all spies/any))
              (is (= sims [::config-1 ::config-2]))))
          (testing "returns a success map"
            (is (http/success? result))))))))

(deftest ^:unit routes-test
  (testing "(routes)"
    (let [sim-routes-spy (spies/and-then [::route-1 ::route-2] [::route-3 ::route4])
          deletes-atom (atom [])
          simulator-1 (reify common/ISimulator
                        (routes [this delete]
                          (swap! deletes-atom conj delete)
                          (sim-routes-spy this)))
          simulator-2 (reify common/ISimulator
                        (routes [this delete]
                          (swap! deletes-atom conj delete)
                          (sim-routes-spy this)))
          simulator-atom (atom {[::key ::1] simulator-1
                                [::key ::2] simulator-2})
          routes-spy (spies/create (constantly ::routes-fn))]
      (with-redefs [simulators/simulators simulator-atom
                    c/routes routes-spy]
        (let [result (simulators/routes)]
          (testing "collects routes"
            (is (spies/called-with? sim-routes-spy simulator-1))
            (is (spies/called-with? sim-routes-spy simulator-2)))
          (testing "passes fn to remove simulator"
            (is (= 2 (count @deletes-atom)))
            (is (= 2 (count @simulator-atom)))
            ((first @deletes-atom) ::key ::1)
            ((second @deletes-atom) ::key ::2)
            (is (empty? @simulator-atom)))
          (testing "makes routes fn"
            (is (spies/called-with? routes-spy ::route-1 ::route-2 ::route-3 ::route4))
            (is (= ::routes-fn result))))))))
