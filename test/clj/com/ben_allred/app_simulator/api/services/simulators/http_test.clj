(ns com.ben-allred.app-simulator.api.services.simulators.http-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.app-simulator.api.services.simulators.common :as common]
    [com.ben-allred.app-simulator.api.services.simulators.http :as http.sim]
    [com.ben-allred.app-simulator.api.services.simulators.routes :as routes.sim]
    [com.ben-allred.app-simulator.api.services.simulators.store.actions :as actions]
    [com.ben-allred.app-simulator.api.services.simulators.store.core :as store]
    [com.ben-allred.app-simulator.api.utils.respond :as respond]
    [com.ben-allred.app-simulator.api.utils.specs :as specs]
    [com.ben-allred.app-simulator.services.navigation :as nav*]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [test.utils.spies :as spies]))

(defn ^:private simulator
  ([]
   (simulator {:method   :http/get
               :path     "/some/path"
               :delay    123
               :response (respond/with [:http.status/ok "[\"some\",\"body\"]" {:header ["some" "header"]}])}))
  ([config]
   (let [dispatch (spies/create)
         get-state (spies/constantly ::state)]
     (with-redefs [store/http-store (spies/constantly {:dispatch  dispatch
                                                       :get-state get-state})]
       [(http.sim/->HttpSimulator ::env ::id config) config dispatch get-state]))))

(deftest ^:unit valid?-test
  (testing "(valid?)"
    (testing "returns true for valid configs"
      (are [config] (http.sim/valid? config)
        {:method   :http/get
         :path     "/some/path"
         :delay    123
         :response {:status  200
                    :headers {:header "some-header"}}}
        {:method   :http/post
         :path     "/"
         :response {:status  200
                    :body    nil}}
        {:method   "http/put"
         :path     "/:param"
         :response {:status  404
                    :body    "a body"
                    :headers {}}}
        {:method   :http/delete
         :path     "/things"
         :response {:status 204}}))

    (testing "returns false for invalid config"
      (are [config] (not (http.sim/valid? config))
        {:method   :http/post
         :response {:status  200
                    :headers []}}
        {:method   :http/patch
         :path     "/"
         :response {:status 200
                    :body   {:some :body}}}
        {:method   :file/get
         :path     "/"
         :response {:status 200}}
        {:method   :http/delete
         :path     :/
         :response {:status 500}}
        {:method   :http/put
         :path     "/"
         :response {:status "200"}}))))

(deftest ^:unit ->HttpSimulator-test
  (testing "(->HttpSimulator)"
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
      (let [init-spy (spies/constantly ::start-action)]
        (with-redefs [actions/init init-spy]
          (let [[_ config dispatch] (simulator)]
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
    (let [sleep-spy (spies/create)]
      (with-redefs [actions/receive (spies/constantly ::receive-action)
                    store/delay (spies/constantly 100)
                    http.sim/sleep sleep-spy
                    store/response (spies/constantly ::response)]
        (let [[sim _ dispatch get-state] (simulator)
              result (common/receive! sim ::request)]
          (testing "gets the state"
            (is (spies/called? get-state)))

          (testing "receives the request"
            (is (spies/called-with? actions/receive ::request))
            (is (spies/called-with? dispatch ::receive-action)))

          (testing "when delay is a positive integer"
            (testing "sleeps"
              (is (spies/called-with? store/delay ::state))
              (is (spies/called-with? sleep-spy 100))))

          (testing "returns response"
            (is (spies/called-with? store/response ::state))
            (is (= ::response result)))

          (testing "when delay is zero"
            (testing "does not sleep"
              (spies/respond-with! store/delay (constantly ::delay))
              (spies/reset! sleep-spy)
              (common/receive! sim ::request)
              (is (spies/never-called? sleep-spy)))))))))

(deftest ^:unit ->HttpSimulator.requests-test
  (testing "(->HttpSimulator.requests)"
    (testing "returns request"
      (with-redefs [store/requests (spies/constantly ::requests)]
        (let [[sim _ _ get-state] (simulator)
              result (common/received sim)]
          (is (spies/called? get-state))
          (is (spies/called-with? store/requests ::state))
          (is (= ::requests result)))))))

(deftest ^:unit ->HttpSimulator.details-test
  (testing "(->HttpSimulator.details)"
    (testing "returns details"
      (with-redefs [store/details (spies/constantly {:config ::details})]
        (let [[sim _ _ get-state] (simulator)
              result (common/details sim)]
          (is (spies/called? get-state))
          (is (spies/called-with? store/details ::state))
          (is (= ::details (:config result)))
          (is (= ::id (:id result))))))))

(deftest ^:unit ->HttpSimulator.method-test
  (testing "(->HttpSimulator.method)"
    (testing "returns unique method"
      (let [[sim] (simulator {:method   :http/get
                              :path     "/some/:param/url/:thing"
                              :response {:status 204}})]
        (is (= :get (common/method sim)))))))

(deftest ^:unit ->HttpSimulator.path-test
  (testing "(->HttpSimulator.path)"
    (testing "returns unique path"
      (let [[sim] (simulator {:method   :http/get
                              :path     "/some/:param/url/:thing"
                              :response {:status 204}})]
        (is (= "/some/:param/url/:thing" (common/path sim)))))))

(deftest ^:unit ->HttpSimulator.reset-test
  (testing "(->HttpSimulator.reset)"
    (testing "resets simulator"
      (let [[sim _ dispatch] (simulator)]
        (common/reset! sim)
        (is (spies/called-with? dispatch actions/reset))))))

(deftest ^:unit ->HttpSimulator.routes-test
  (testing "(->HttpSimulator.routes)"
    (with-redefs [routes.sim/http-sim->routes (spies/constantly ::routes)]
      (testing "converts simulator to routes"
        (let [[sim] (simulator)
              result (common/routes sim)]
          (is (spies/called-with? routes.sim/http-sim->routes ::env sim))
          (is (= ::routes result)))))))

(deftest ^:unit ->HttpSimulator.reset-requests-test
  (testing "(->HttpSimulator.reset-requests)"
    (testing "resets simulator's requests"
      (let [[sim _ dispatch] (simulator)]
        (common/partially-reset! sim :http/requests)
        (is (spies/called-with? dispatch actions/reset-requests))))))

(deftest ^:unit ->HttpSimulator.reset-response-test
  (testing "(->HttpSimulator.reset-response)"
    (testing "resets simulator's response"
      (let [[sim _ dispatch] (simulator)]
        (common/partially-reset! sim :http/response)
        (is (spies/called-with? dispatch actions/reset-response))))))

(deftest ^:unit ->HttpSimulator.change-test
  (testing "(->HttpSimulator.change)"
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

(deftest ^:unit ->HttpSimulator.equals-test
  (testing "(->HttpSimulator.equals"
    (testing "when the path sections match"
      (with-redefs [nav*/path-matcher (constantly (constantly true))]
        (are [config-1 config-2 does?] (let [[sim-1] (-> config-1
                                                         (assoc :response {:status 204})
                                                         (simulator))
                                             sim-2 (when (map? config-2)
                                                     (reify common/IIdentify
                                                       (method [_]
                                                         (:method config-2))
                                                       (path [_]
                                                         (:path config-2))))]
                                         (= does? (= sim-1 sim-2)))
          {:method :http/get :path "/path"} {:method :get :path ::path} true
          {:method :http/post :path "/path"} {:method :post :path ::path} true
          {:method :http/patch :path "/path"} {:method :delete :path ::path} false
          {:method :http/put :path "/path"} #{} false
          {:method :http/put :path "/path"} "" false
          {:method :http/put :path "/path"} nil false)))

    (testing "when the path sections do not match"
      (with-redefs [nav*/path-matcher (constantly (constantly false))]
        (are [config-1 config-2] (let [[sim-1] (-> config-1
                                                   (assoc :response {:status 204})
                                                   (simulator))
                                       sim-2 (when (map? config-2)
                                               (reify common/IIdentify
                                                 (method [_]
                                                   (:method config-2))
                                                 (path [_]
                                                   (:path config-2))))]
                                   (not= sim-1 sim-2))
          {:method :http/get :path "/path"} {:method :get :path ::path}
          {:method :http/post :path "/path"} {:method :post :path ::path})))))
