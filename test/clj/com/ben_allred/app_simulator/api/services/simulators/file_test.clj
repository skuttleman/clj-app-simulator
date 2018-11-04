(ns com.ben-allred.app-simulator.api.services.simulators.file-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.app-simulator.api.services.simulators.common :as common]
    [com.ben-allred.app-simulator.api.services.simulators.file :as file.sim]
    [com.ben-allred.app-simulator.api.services.simulators.routes :as routes.sim]
    [com.ben-allred.app-simulator.api.services.simulators.store.actions :as actions]
    [com.ben-allred.app-simulator.api.services.simulators.store.core :as store]
    [com.ben-allred.app-simulator.api.utils.specs :as specs]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [com.ben-allred.app-simulator.utils.uuids :as uuids]
    [test.utils.spies :as spies]))

(defn ^:private simulator
  ([]
   (simulator {:method   :file/get
               :path     "/some/path"
               :delay    123
               :response {:status  200
                          :file    (uuids/random)
                          :headers {:header "some-header"}}}))
  ([config]
   (let [dispatch (spies/create)
         get-state (spies/constantly ::state)]
     (with-redefs [store/file-store (spies/constantly {:dispatch  dispatch
                                                       :get-state get-state})]
       [(file.sim/->FileSimulator ::env ::id config) config dispatch get-state]))))

(deftest ^:unit valid?-test
  (testing "(valid?)"
    (testing "returns true for valid configs"
      (are [config] (file.sim/valid? config)
        {:method   :file/get
         :path     "/some/path"
         :delay    123
         :response {:status  200
                    :file    (uuids/random)
                    :headers {:header "some-header"}}}
        {:method   :file/post
         :path     "/"
         :response {:status  200
                    :file    (uuids/random)
                    :headers {}}}
        {:method   "file/put"
         :path     "/:param"
         :response {:status 404
                    :file   (str (uuids/random))}}))

    (testing "returns false for invalid config"
      (are [config] (not (file.sim/valid? config))
        {:method   :file/post
         :response {:status 200
                    :file   (uuids/random)}}
        {:method   ::get
         :path     "/"
         :response {:status 200
                    :file   (uuids/random)}}
        {:method   :file/post
         :path     :/
         :response {:status 200
                    :file   (uuids/random)}}
        {:method   :file/delete
         :path     "/"
         :response {:file (uuids/random)}}
        {:method   :file/put
         :path     "/"
         :response {:status 200}}
        {:method   :file/get
         :path     "/"
         :response {:status  201
                    :file    (uuids/random)
                    :headers []}}))))

(deftest ^:unit ->FileSimulator-test
  (testing "(->FileSimulator)"
    (testing "when config is valid"
      (testing "returns a simulator"
        (let [[sim] (simulator)]
          (doseq [protocol [common/IIdentify common/IReceive common/IReset
                            common/IRoute common/IPartiallyReset]]
            (is (satisfies? protocol sim))))))

    (testing "when config is invalid"
      (testing "returns nil"
        (is (nil? (first (simulator {}))))))

    (testing "initializes the store"
      (with-redefs [actions/init (spies/constantly ::start-action)]
        (let [[_ config dispatch] (simulator)]
          (is (spies/called-with? actions/init config))
          (is (spies/called-with? dispatch ::start-action)))))))

(deftest ^:unit ->FileSimulator.start-test
  (testing "(->FileSimulator.start)"
    (testing "does not explode"
      (common/start! (first (simulator))))))

(deftest ^:unit ->FileSimulator.stop-test
  (testing "(->FileSimulator.stop)"
    (testing "does not explode"
      (common/stop! (first (simulator))))))

(deftest ^:unit ->FileSimulator.receive-test
  (testing "(->FileSimulator.receive)"
    (let [sleep-spy (spies/create)]
      (with-redefs [actions/receive (spies/constantly ::receive-action)
                    store/delay (spies/constantly 100)
                    file.sim/sleep sleep-spy
                    store/file-response (spies/constantly ::response)]
        (let [[sim _ dispatch get-state] (simulator)
              result (common/receive! sim ::request)]
          (is (spies/called? get-state))
          (testing "receives the request"
            (is (spies/called-with? actions/receive ::request))
            (is (spies/called-with? dispatch ::receive-action)))
          (testing "when delay is a positive integer"
            (testing "sleeps"
              (is (spies/called-with? store/delay ::state))
              (is (spies/called-with? sleep-spy 100))))
          (testing "returns response"
            (is (spies/called-with? store/file-response ::env ::state))
            (is (= ::response result)))
          (testing "when delay is zero"
            (testing "does not sleep"
              (spies/respond-with! store/delay (constantly ::delay))
              (spies/reset! sleep-spy)
              (common/receive! sim ::request)
              (is (spies/never-called? sleep-spy)))))))))

(deftest ^:unit ->FileSimulator.requests-test
  (testing "(->FileSimulator.requests)"
    (testing "returns request"
      (with-redefs [store/requests (spies/constantly ::requests)]
        (let [[sim _ _ get-state] (simulator)
              result (common/received sim)]
          (is (spies/called? get-state))
          (is (spies/called-with? store/requests ::state))
          (is (= ::requests result)))))))

(deftest ^:unit ->FileSimulator.details-test
  (testing "(->FileSimulator.details)"
    (testing "returns details"
      (with-redefs [store/details (spies/constantly {:config ::details})]
        (let [[sim _ _ get-state] (simulator)
              result (common/details sim)]
          (is (spies/called? get-state))
          (is (spies/called-with? store/details ::state))
          (is (= ::details (:config result)))
          (is (= ::id (:id result))))))))

(deftest ^:unit ->FileSimulator.identifier-test
  (testing "(->FileSimulator.identifier)"
    (testing "returns unique identifier"
      (let [[sim] (simulator {:method   :file/post
                              :path     "/some/:param/url/:id/action"
                              :response {:status 204
                                         :file   (uuids/random)}})]
        (let [result (common/identifier sim)]
          (is (= [:post "/some/*/url/*/action"] result)))))))

(deftest ^:unit ->FileSimulator.reset-test
  (testing "(->FileSimulator.reset)"
    (testing "resets simulator"
      (let [[sim _ dispatch] (simulator)]
        (common/reset! sim)
        (is (spies/called-with? dispatch actions/reset))))))

(deftest ^:unit ->FileSimulator.routes-test
  (testing "(->FileSimulator.routes)"
    (with-redefs [routes.sim/file-sim->routes (spies/constantly ::routes)]
      (testing "converts simulator to routes"
        (let [[sim] (simulator)
              result (common/routes sim)]
          (is (spies/called-with? routes.sim/file-sim->routes ::env sim))
          (is (= ::routes result)))))))

(deftest ^:unit ->FileSimulator.reset-requests-test
  (testing "(->FileSimulator.reset-requests)"
    (testing "resets simulator's requests"
      (let [[sim _ dispatch] (simulator)]
        (common/partially-reset! sim :file/requests)
        (is (spies/called-with? dispatch actions/reset-requests))))))

(deftest ^:unit ->FileSimulator.reset-response-test
  (testing "(->FileSimulator.reset-response)"
    (testing "resets simulator's response"
      (let [[sim _ dispatch] (simulator)]
        (common/partially-reset! sim :file/response)
        (is (spies/called-with? dispatch actions/reset-response))))))

(deftest ^:unit ->FileSimulator.change-test
  (testing "(->FileSimulator.change)"
    (let [[sim _ dispatch] (simulator)
          config {:delay 100 :response {:body "{\"some\":\"json\"}"} :extra ::junk}]
      (testing "changes changeable config properties"
        (with-redefs [actions/change (spies/constantly ::action)]
          (common/reset! sim (assoc config :method ::method :path ::path))
          (is (spies/called-with? actions/change config))
          (is (spies/called-with? dispatch ::action))))

      (testing "when config is bad"
        (with-redefs [specs/explain (spies/constantly ::reasons)]
          (testing "throws exception"
            (is (thrown? Throwable (common/reset! sim ::bad-config))))
          (testing "explains spec errors"
            (try
              (common/reset! sim ::bad-config)
              (catch Throwable ex
                (is (= ::reasons (:problems (ex-data ex))))))))))))
