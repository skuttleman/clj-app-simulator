(ns com.ben-allred.clj-app-simulator.api.services.simulators.http-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.api.services.simulators.http :as http.sim]
            [com.ben-allred.clj-app-simulator.api.services.simulators.store.core :as store]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
            [com.ben-allred.clj-app-simulator.api.services.simulators.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.api.services.simulators.routes :as routes.sim]
            [com.ben-allred.clj-app-simulator.api.utils.respond :as respond]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(defn ^:private simulator
  ([] (simulator {:method   :http/get
                  :path     "/some/path"
                  :delay    123
                  :response (respond/with [:ok "[\"some\",\"body\"]" {:header ["some" "header"]}])}))
  ([config]
   (let [dispatch (spies/create)
         get-state (spies/create (constantly ::state))
         spy (spies/create (constantly {:dispatch  dispatch
                                        :get-state get-state}))]
     (with-redefs [store/http-store spy]
       [(http.sim/->HttpSimulator ::id config) spy config dispatch get-state]))))

(deftest ^:unit ->HttpSimulator-test
  (testing "(->HttpSimulator)"
    (testing "when config is valid"
      (testing "creates an http store"
        (is (spies/called-with? (second (simulator)))))
      (testing "returns a simulator"
        (is (first (simulator)))))
    (testing "when config is invalid"
      (testing "does not return a simulator"
        (is (not (first (simulator {}))))))))

(deftest ^:unit ->HttpSimulator.start-test
  (testing "(->HttpSimulator.start)"
    (testing "initializes the store"
      (let [[sim _ config dispatch] (simulator)
            init-spy (spies/create (constantly ::start-action))]
        (with-redefs [actions/init init-spy]
          (common/start sim)
          (is (spies/called-with? init-spy config))
          (is (spies/called-with? dispatch ::start-action)))))))

(deftest ^:unit ->HttpSimulator.stop-test
  (testing "(->HttpSimulator.stop)"
    (testing "does not explode"
      (common/stop (first (simulator))))))

(deftest ^:unit ->HttpSimulator.receive-test
  (testing "(->HttpSimulator.receive)"
    (let [[sim _ _ dispatch get-state] (simulator)
          receive-spy (spies/create (constantly ::receive-action))
          delay-spy (spies/create (constantly 100))
          sleep-spy (spies/create)
          response-spy (spies/create (constantly ::response))]
      (with-redefs [actions/receive receive-spy
                    store/delay delay-spy
                    http.sim/sleep sleep-spy
                    store/response response-spy]
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
            (is (spies/called-with? response-spy ::state))
            (is (= ::response result))))
        (testing "when delay is zero"
          (testing "does not sleep"
            (spies/respond-with! delay-spy (constantly ::delay))
            (spies/reset! sleep-spy)
            (common/receive sim ::request)
            (is (spies/never-called? sleep-spy))))))))

(deftest ^:unit ->HttpSimulator.requests-test
  (testing "(->HttpSimulator.requests)"
    (testing "returns request"
      (let [[sim _ _ _ get-state] (simulator)
            requests-spy (spies/create (constantly ::requests))]
        (with-redefs [store/requests requests-spy]
          (let [result (common/requests sim)]
            (is (spies/called? get-state))
            (is (spies/called-with? requests-spy ::state))
            (is (= ::requests result))))))))

(deftest ^:unit ->HttpSimulator.details-test
  (testing "(->HttpSimulator.details)"
    (testing "returns request"
      (let [[sim _ _ _ get-state] (simulator)
            details-spy (spies/create (constantly {:config ::details}))]
        (with-redefs [store/details details-spy]
          (let [result (common/details sim)]
            (is (spies/called? get-state))
            (is (spies/called-with? details-spy ::state))
            (is (= ::details (:config result)))
            (is (= ::id (:id result)))))))))

(deftest ^:unit ->HttpSimulator.reset-test
  (testing "(->HttpSimulator.reset)"
    (testing "resets simulator"
      (let [[sim _ _ dispatch] (simulator)]
        (common/reset sim)
        (is (spies/called-with? dispatch actions/reset))))))

(deftest ^:unit ->HttpSimulator.routes-test
  (testing "(->HttpSimulator.routes)"
    (let [[sim] (simulator)
          config-spy (spies/create (constantly ::routes))]
      (with-redefs [routes.sim/http-sim->routes config-spy]
        (testing "converts simulator to routes"
          (let [result (common/routes sim ::delete)]
            (is (spies/called-with? config-spy sim ::delete))
            (is (= ::routes result))))))))

(deftest ^:unit ->HttpSimulator.reset-requests-test
  (testing "(->HttpSimulator.reset-requests)"
    (testing "resets simulator's requests"
      (let [[sim _ _ dispatch] (simulator)]
        (common/reset-requests sim)
        (is (spies/called-with? dispatch actions/reset-requests))))))

(deftest ^:unit ->HttpSimulator.reset-response-test
  (testing "(->HttpSimulator.reset-response)"
    (testing "resets simulator's response"
      (let [[sim _ _ dispatch] (simulator)]
        (common/reset-response sim)
        (is (spies/called-with? dispatch actions/reset-response))))))

(deftest ^:unit ->HttpSimulator.change-test
  (testing "(->HttpSimulator.change)"
    (let [[sim _ _ dispatch] (simulator)
          change-spy (spies/create (constantly ::action))
          config {:delay 100 :response {:body "{\"some\":\"json\"}"}}]
      (with-redefs [actions/change change-spy]
        (testing "changes changeable config properties"
          (common/change sim (assoc config :extra ::junk))
          (is (spies/called-with? change-spy config))
          (is (spies/called-with? dispatch ::action)))
        (testing "when config is bad"
          (with-redefs [http.sim/why-not-update? (spies/create (constantly ::reasons))]
            (testing "throws exception"
              (is (thrown? Throwable (common/change sim ::bad-config))))
            (testing "explains spec errors"
              (try
                (common/change sim ::bad-config)
                (catch Throwable ex
                  (is (= ::reasons (:problems (ex-data ex)))))))))))))
