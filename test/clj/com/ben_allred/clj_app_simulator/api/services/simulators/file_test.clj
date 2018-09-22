(ns com.ben-allred.clj-app-simulator.api.services.simulators.file-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.api.services.simulators.file :as file.sim]
            [com.ben-allred.clj-app-simulator.api.services.simulators.store.core :as store]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
            [com.ben-allred.clj-app-simulator.api.services.simulators.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.api.services.simulators.routes :as routes.sim]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.api.services.resources.core :as resources]))

(defn ^:private simulator
  ([] (simulator {:method   :file/get
                  :path     "/some/path"
                  :delay    123
                  :response {:status 200
                             :file ::123
                             :headers {:header "some-header"}}}))
  ([config]
   (let [resources-spy (spies/constantly true)
         dispatch (spies/create)
         get-state (spies/constantly ::state)
         spy (spies/constantly {:dispatch  dispatch
                                :get-state get-state})]
     (with-redefs [store/file-store spy
                   resources/has-upload? resources-spy]
       [(file.sim/->FileSimulator ::env ::id config) spy config dispatch get-state]))))

(deftest ^:unit ->FileSimulator-test
  (testing "(->FileSimulator)"
    (testing "when config is valid"
      (testing "creates an file store"
        (is (spies/called-with? (second (simulator)))))

      (testing "returns a simulator"
        (let [[sim] (simulator)]
          (is (satisfies? common/ISimulator sim))
          (is (satisfies? common/IHTTPSimulator sim)))))

    (testing "when config is invalid"
      (testing "returns nil"
        (is (nil? (first (simulator {}))))))

    (testing "initializes the store"
      (let [init-spy (spies/constantly ::start-action)]
        (with-redefs [actions/init init-spy]
          (let [[_ _ config dispatch] (simulator)]
            (is (spies/called-with? init-spy config))
            (is (spies/called-with? dispatch ::start-action))))))))

(deftest ^:unit ->FileSimulator.start-test
  (testing "(->FileSimulator.start)"
    (testing "does not explode"
      (common/start (first (simulator))))))

(deftest ^:unit ->FileSimulator.stop-test
  (testing "(->FileSimulator.stop)"
    (testing "does not explode"
      (common/stop (first (simulator))))))

(deftest ^:unit ->FileSimulator.receive-test
  (testing "(->FileSimulator.receive)"
    (let [[sim _ _ dispatch get-state] (simulator)
          receive-spy (spies/constantly ::receive-action)
          delay-spy (spies/constantly 100)
          sleep-spy (spies/create)
          response-spy (spies/constantly ::response)]
      (with-redefs [actions/receive receive-spy
                    store/delay delay-spy
                    file.sim/sleep sleep-spy
                    store/file-response response-spy]
        (let [result (common/receive sim ::request)]
          (is (spies/called? get-state))
          (testing "receives the request"
            (is (spies/called-with? receive-spy ::request))
            (is (spies/called-with? dispatch ::receive-action)))
          (testing "when delay is a positive integer"
            (testing "sleeps"
              (is (spies/called-with? delay-spy ::state))
              (is (spies/called-with? sleep-spy 100))))
          (testing "returns response"
            (is (spies/called-with? response-spy ::env ::state))
            (is (= ::response result))))
        (testing "when delay is zero"
          (testing "does not sleep"
            (spies/respond-with! delay-spy (constantly ::delay))
            (spies/reset! sleep-spy)
            (common/receive sim ::request)
            (is (spies/never-called? sleep-spy))))))))

(deftest ^:unit ->FileSimulator.requests-test
  (testing "(->FileSimulator.requests)"
    (testing "returns request"
      (let [[sim _ _ _ get-state] (simulator)
            requests-spy (spies/constantly ::requests)]
        (with-redefs [store/requests requests-spy]
          (let [result (common/requests sim)]
            (is (spies/called? get-state))
            (is (spies/called-with? requests-spy ::state))
            (is (= ::requests result))))))))

(deftest ^:unit ->FileSimulator.details-test
  (testing "(->FileSimulator.details)"
    (testing "returns details"
      (let [[sim _ _ _ get-state] (simulator)
            details-spy (spies/constantly {:config ::details})]
        (with-redefs [store/details details-spy]
          (let [result (common/details sim)]
            (is (spies/called? get-state))
            (is (spies/called-with? details-spy ::state))
            (is (= ::details (:config result)))
            (is (= ::id (:id result)))))))))

(deftest ^:unit ->FileSimulator.identifier-test
  (testing "(->FileSimulator.identifier)"
    (testing "returns unique identifier"
      (let [[sim] (simulator {:method   :file/post
                              :path     "/some/:param/url/:id/action"
                              :response {:status 204
                                         :file ::123}})]
        (let [result (common/identifier sim)]
          (is (= [:post "/some/*/url/*/action"] result)))))))

(deftest ^:unit ->FileSimulator.reset-test
  (testing "(->FileSimulator.reset)"
    (testing "resets simulator"
      (let [[sim _ _ dispatch] (simulator)]
        (common/reset sim)
        (is (spies/called-with? dispatch actions/reset))))))

(deftest ^:unit ->FileSimulator.routes-test
  (testing "(->FileSimulator.routes)"
    (let [[sim] (simulator)
          config-spy (spies/constantly ::routes)]
      (with-redefs [routes.sim/file-sim->routes config-spy]
        (testing "converts simulator to routes"
          (let [result (common/routes sim)]
            (is (spies/called-with? config-spy ::env sim))
            (is (= ::routes result))))))))

(deftest ^:unit ->FileSimulator.reset-requests-test
  (testing "(->FileSimulator.reset-requests)"
    (testing "resets simulator's requests"
      (let [[sim _ _ dispatch] (simulator)]
        (common/reset-requests sim)
        (is (spies/called-with? dispatch actions/reset-requests))))))

(deftest ^:unit ->FileSimulator.reset-response-test
  (testing "(->FileSimulator.reset-response)"
    (testing "resets simulator's response"
      (let [[sim _ _ dispatch] (simulator)]
        (common/reset-response sim)
        (is (spies/called-with? dispatch actions/reset-response))))))

(deftest ^:unit ->FileSimulator.change-test
  (testing "(->FileSimulator.change)"
    (let [[sim _ _ dispatch] (simulator)
          change-spy (spies/constantly ::action)
          config {:delay 100 :response {:body "{\"some\":\"json\"}"} :extra ::junk}]
      (with-redefs [actions/change change-spy]
        (testing "changes changeable config properties"
          (common/change sim (assoc config :method ::method :path ::path))
          (is (spies/called-with? change-spy config))
          (is (spies/called-with? dispatch ::action)))
        (testing "when config is bad"
          (with-redefs [file.sim/why-not-update? (spies/constantly ::reasons)]
            (testing "throws exception"
              (is (thrown? Throwable (common/change sim ::bad-config))))
            (testing "explains spec errors"
              (try
                (common/change sim ::bad-config)
                (catch Throwable ex
                  (is (= ::reasons (:problems (ex-data ex)))))))))))))
