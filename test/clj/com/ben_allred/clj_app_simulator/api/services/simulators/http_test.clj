(ns com.ben-allred.clj-app-simulator.api.services.simulators.http-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
    [com.ben-allred.clj-app-simulator.api.services.simulators.http :as http.sim]
    [com.ben-allred.clj-app-simulator.api.services.simulators.routes :as routes.sim]
    [com.ben-allred.clj-app-simulator.api.services.simulators.store.actions :as actions]
    [com.ben-allred.clj-app-simulator.api.services.simulators.store.core :as store]
    [com.ben-allred.clj-app-simulator.api.utils.respond :as respond]
    [com.ben-allred.clj-app-simulator.utils.logging :as log]
    [test.utils.spies :as spies]))

(defn ^:private simulator
  ([] (simulator {:method   :http/get
                  :path     "/some/path"
                  :delay    123
                  :response (respond/with [:ok "[\"some\",\"body\"]" {:header ["some" "header"]}])}))
  ([config]
   (let [dispatch (spies/create)
         get-state (spies/constantly ::state)
         spy (spies/constantly {:dispatch  dispatch
                                :get-state get-state})]
     (with-redefs [store/http-store spy]
       [(http.sim/->HttpSimulator ::env ::id config) spy config dispatch get-state]))))

(deftest ^:unit ->HttpSimulator-test
  (testing "(->HttpSimulator)"
    (testing "when config is valid"
      (testing "creates an http store"
        (is (spies/called-with? (second (simulator)))))

      (testing "returns a simulator"
        (let [[sim] (simulator)]
          (doseq [protocol [common/IIdentify common/IReceive common/IReset
                            common/IRoute common/IPartiallyReset]]
            (is (satisfies? protocol sim))))))

    (testing "when config is invalid"
      (testing "returns nil"
        (is (nil? (first (simulator {}))))))

    (testing "initializes the store"
      (let [init-spy (spies/constantly ::start-action)]
        (with-redefs [actions/init init-spy]
          (let [[_ _ config dispatch] (simulator)]
            (is (spies/called-with? init-spy config))
            (is (spies/called-with? dispatch ::start-action))))))))

(deftest ^:unit ->HttpSimulator.start-test
  (testing "(->HttpSimulator.start)"
    (testing "does not explode"
      (common/start! (first (simulator))))))

(deftest ^:unit ->HttpSimulator.stop-test
  (testing "(->HttpSimulator.stop)"
    (testing "does not explode"
      (common/stop! (first (simulator))))))

(deftest ^:unit ->HttpSimulator.receive-test
  (testing "(->HttpSimulator.receive)"
    (let [[sim _ _ dispatch get-state] (simulator)
          receive-spy (spies/constantly ::receive-action)
          delay-spy (spies/constantly 100)
          sleep-spy (spies/create)
          response-spy (spies/constantly ::response)]
      (with-redefs [actions/receive receive-spy
                    store/delay delay-spy
                    http.sim/sleep sleep-spy
                    store/response response-spy]
        (let [result (common/receive! sim ::request)]
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
            (common/receive! sim ::request)
            (is (spies/never-called? sleep-spy))))))))

(deftest ^:unit ->HttpSimulator.requests-test
  (testing "(->HttpSimulator.requests)"
    (testing "returns request"
      (let [[sim _ _ _ get-state] (simulator)
            requests-spy (spies/constantly ::requests)]
        (with-redefs [store/requests requests-spy]
          (let [result (common/received sim)]
            (is (spies/called? get-state))
            (is (spies/called-with? requests-spy ::state))
            (is (= ::requests result))))))))

(deftest ^:unit ->HttpSimulator.details-test
  (testing "(->HttpSimulator.details)"
    (testing "returns details"
      (let [[sim _ _ _ get-state] (simulator)
            details-spy (spies/constantly {:config ::details})]
        (with-redefs [store/details details-spy]
          (let [result (common/details sim)]
            (is (spies/called? get-state))
            (is (spies/called-with? details-spy ::state))
            (is (= ::details (:config result)))
            (is (= ::id (:id result)))))))))

(deftest ^:unit ->HttpSimulator.identifier-test
  (testing "(->HttpSimulator.identifier)"
    (testing "returns unique identifier"
      (let [[sim] (simulator {:method   :http/get
                              :path     "/some/:param/url/:thing"
                              :response {:status 204}})]
        (let [result (common/identifier sim)]
          (is (= [:get "/some/*/url/*"] result)))))))

(deftest ^:unit ->HttpSimulator.reset-test
  (testing "(->HttpSimulator.reset)"
    (testing "resets simulator"
      (let [[sim _ _ dispatch] (simulator)]
        (common/reset! sim)
        (is (spies/called-with? dispatch actions/reset))))))

(deftest ^:unit ->HttpSimulator.routes-test
  (testing "(->HttpSimulator.routes)"
    (let [[sim] (simulator)
          config-spy (spies/constantly ::routes)]
      (with-redefs [routes.sim/http-sim->routes config-spy]
        (testing "converts simulator to routes"
          (let [result (common/routes sim)]
            (is (spies/called-with? config-spy ::env sim))
            (is (= ::routes result))))))))

(deftest ^:unit ->HttpSimulator.reset-requests-test
  (testing "(->HttpSimulator.reset-requests)"
    (testing "resets simulator's requests"
      (let [[sim _ _ dispatch] (simulator)]
        (common/partially-reset! sim :http/requests)
        (is (spies/called-with? dispatch actions/reset-requests))))))

(deftest ^:unit ->HttpSimulator.reset-response-test
  (testing "(->HttpSimulator.reset-response)"
    (testing "resets simulator's response"
      (let [[sim _ _ dispatch] (simulator)]
        (common/partially-reset! sim :http/response)
        (is (spies/called-with? dispatch actions/reset-response))))))

(deftest ^:unit ->HttpSimulator.change-test
  (testing "(->HttpSimulator.change)"
    (let [[sim _ _ dispatch] (simulator)
          change-spy (spies/constantly ::action)
          config {:delay 100 :response {:body "{\"some\":\"json\"}"} :extra ::junk}]
      (with-redefs [actions/change change-spy]
        (testing "changes changeable config properties"
          (common/reset! sim (assoc config :method ::method :path ::path))
          (is (spies/called-with? change-spy config))
          (is (spies/called-with? dispatch ::action)))
        (testing "when config is bad"
          (with-redefs [http.sim/why-not-update? (spies/constantly ::reasons)]
            (testing "throws exception"
              (is (thrown? Throwable (common/reset! sim ::bad-config))))
            (testing "explains spec errors"
              (try
                (common/reset! sim ::bad-config)
                (catch Throwable ex
                  (is (= ::reasons (:problems (ex-data ex)))))))))))))
