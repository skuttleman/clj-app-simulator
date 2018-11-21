(ns com.ben-allred.app-simulator.api.services.simulators.ws-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.app-simulator.api.services.activity :as activity]
    [com.ben-allred.app-simulator.api.services.simulators.common :as common]
    [com.ben-allred.app-simulator.api.services.simulators.routes :as routes.sim]
    [com.ben-allred.app-simulator.api.services.simulators.store.actions :as actions]
    [com.ben-allred.app-simulator.api.services.simulators.store.core :as store]
    [com.ben-allred.app-simulator.api.services.simulators.ws :as ws.sim]
    [com.ben-allred.app-simulator.services.navigation :as nav*]
    [com.ben-allred.app-simulator.utils.uuids :as uuids]
    [immutant.web.async :as web.async]
    [test.utils.spies :as spies]))

(defn ^:private simulator
  ([]
   (simulator {:method :ws/ws
               :path   "/some/path"}))
  ([config]
   (let [dispatch (spies/create)
         get-state (spies/constantly ::state)]
     (with-redefs [store/ws-store (spies/constantly {:dispatch  dispatch
                                                     :get-state get-state})]
       [(ws.sim/->WsSimulator ::env ::id config) config dispatch get-state]))))

(deftest ^:unit valid?-test
  (testing "(valid?)"
    (testing "recognizes valid configs"
      (are [config] (ws.sim/valid? config)
        {:path "/some/path" :method :ws/ws}
        {:path "/" :method "ws/ws"}
        {:path "/:id" :method :ws/ws}
        {:path "/some/path" :method :ws/ws}
        {:path "/this/:is/also/:valid" :method "ws/ws"}))

    (testing "recognizes invalid configs"
      (are [config] (not (ws.sim/valid? config))
        {}
        {:path "/valid/path"}
        {:method :ws/ws}
        {:path nil :method nil}
        {:path "" :method :ws}
        {:path "/" :method :method}
        {:path "/$$$" :method :ws/ws}
        {:path "/path/" :method :ws/ws}
        {:path ::path :method ::method}))))

(deftest ^:unit on-open-test
  (testing "(on-open)"
    (let [dispatch-spy (spies/create)
          uuid (uuids/random)]
      (with-redefs [actions/connect (spies/constantly ::action)
                    activity/publish (spies/create)
                    common/details (spies/constantly {::some ::details})
                    uuids/random (constantly uuid)]
        (testing "when the connection is opened"
          (ws.sim/on-open ::env ::simulator ::request {:dispatch dispatch-spy} ::ws)
          (testing "dispatches an action"
            (is (spies/called-with? actions/connect uuid ::ws))
            (is (spies/called-with? dispatch-spy ::action)))

          (testing "publishes an event"
            (is (spies/called-with? common/details ::simulator))
            (is (spies/called-with? activity/publish ::env :simulators.ws/connect {:simulator {::some ::details} :socket-id uuid}))))))))

(deftest ^:unit on-message-test
  (testing "(on-message)"
    (let [get-state-spy (spies/constantly ::state)]
      (with-redefs [actions/find-socket-id (spies/constantly ::socket-id)
                    common/receive! (spies/create)]
        (testing "when the socket-id is found"
          (spies/reset! actions/find-socket-id common/receive! get-state-spy)
          (ws.sim/on-message ::simulator
                             {:query-params ::query-params :route-params ::route-params :headers ::headers}
                             {:get-state get-state-spy}
                             ::ws
                             ::message)
          (testing "receives the request"
            (is (spies/called? get-state-spy))
            (is (spies/called-with? actions/find-socket-id ::state ::ws))
            (is (spies/called-with? common/receive!
                                    ::simulator
                                    {:headers      ::headers
                                     :query-params ::query-params
                                     :route-params ::route-params
                                     :socket-id    ::socket-id
                                     :body         ::message}))))

        (testing "when the socket-id is not found"
          (spies/reset! actions/find-socket-id common/receive! get-state-spy)
          (spies/respond-with! actions/find-socket-id (constantly nil))
          (testing "does not receive the request"
            (spies/never-called? common/receive!)))))))

(deftest ^:unit on-close-test
  (testing "(on-close)"
    (let [get-state-spy (spies/constantly ::state)
          dispatch-spy (spies/create)]
      (with-redefs [actions/find-socket-id (spies/constantly ::socket-id)
                    actions/remove-socket (spies/constantly ::action)
                    activity/publish (spies/create)
                    common/details (spies/constantly {::some ::details})]
        (testing "when the socket-id is found"
          (spies/reset! actions/find-socket-id get-state-spy actions/remove-socket dispatch-spy activity/publish common/details)
          (ws.sim/on-close ::env
                           ::simulator
                           ::request
                           {:dispatch dispatch-spy :get-state get-state-spy}
                           ::ws
                           ::reason)

          (testing "dispatches an event"
            (is (spies/called-with? actions/find-socket-id ::state ::ws))
            (is (spies/called-with? actions/remove-socket ::socket-id))
            (is (spies/called-with? dispatch-spy ::action)))

          (testing "publishes an event"
            (is (spies/called-with? common/details ::simulator))
            (is (spies/called-with? activity/publish
                                    ::env
                                    :simulators.ws/disconnect
                                    {:simulator {::some ::details} :socket-id ::socket-id}))))

        (testing "when the socket-id is not found"
          (spies/reset! actions/find-socket-id get-state-spy actions/remove-socket dispatch-spy activity/publish common/details)
          (spies/respond-with! actions/find-socket-id (constantly nil))
          (testing "does not dispatch an event"
            (is (spies/never-called? actions/remove-socket))
            (is (spies/never-called? dispatch-spy)))

          (testing "does not publish an event"
            (is (spies/never-called? activity/publish))))))))

(deftest ^:unit ->WsSimulator-test
  (testing "(->WsSimulator)"
    (with-redefs [actions/init (spies/constantly ::action)]
      (testing "when the config is valid"
        (let [[sim config dispatch] (simulator)]
          (testing "initializes the store"
            (is (spies/called-with? actions/init config))
            (is (spies/called-with? dispatch ::action)))

          (testing "returns a simulator"
            (doseq [protocol [common/IRun common/IIdentify common/IReceive
                              common/IReset common/IRoute
                              common/IPartiallyReset common/ICommunicate]]
              (is (satisfies? protocol sim))))))

      (testing "when the config is invalid"
        (let [[sim _ dispatch] (simulator {})]
          (testing "does not initialize the store"
            (is (spies/never-called? dispatch)))

          (testing "returns nil"
            (is (nil? sim))))))))

(deftest ^:unit ->WsSimulator.start-test
  (testing "(->WsSimulator.start)"
    (let [[sim] (simulator)]
      (testing "does not explode"
        (common/start! sim)))))

(deftest ^:unit ->WsSimulator.stop-test
  (testing "(->WsSimulator.stop)"
    (let [[sim _ dispatch] (simulator)]
      (common/stop! sim)
      (is (spies/called-with? dispatch actions/disconnect-all)))))

(deftest ^:unit ->WsSimulator.receive-test
  (testing "(->WsSimulator.receive)"
    (with-redefs [actions/receive (spies/constantly ::action)
                  routes.sim/receive (spies/create)]
      (testing "receives messages"
        (let [[sim _ dispatch] (simulator)
              request {::some ::things :socket-id ::socket-id :message-id ::message-id}]
          (common/receive! sim request)
          (is (spies/called-with? actions/receive request))
          (is (spies/called-with? dispatch ::action))
          (is (spies/called-with? routes.sim/receive ::env sim)))))))

(deftest ^:unit ->WsSimulator.requests-test
  (testing "(->WsSimulator.requests)"
    (with-redefs [store/requests (spies/constantly ::messages)]
      (testing "returns requests"
        (let [[sim _ _ get-state] (simulator)
              result (common/received sim)]
          (is (spies/called-with? get-state))
          (is (spies/called-with? store/requests ::state))
          (is (= ::messages result)))))))

(deftest ^:unit ->WsSimulator.details-test
  (testing "(->WsSimulator.details)"
    (with-redefs [store/details (spies/constantly {::some ::details})]
      (testing "returns details"
        (let [[sim _ _ get-state] (simulator)
              result (common/details sim)]
          (is (spies/called-with? get-state))
          (is (spies/called-with? store/details ::state))
          (is (= {::some ::details :id ::id} result)))))))

(deftest ^:unit ->WsSimulator.method-test
  (testing "(->WsSimulator.method)"
    (testing "returns unique method"
      (let [[sim] (simulator {:method :ws/ws
                              :path   "/some/:param"})]
        (is (= :ws (common/method sim)))))))

(deftest ^:unit ->WsSimulator.path-test
  (testing "(->WsSimulator.path)"
    (testing "returns unique path"
      (let [[sim] (simulator {:method :ws/ws
                              :path   "/some/:param"})]
        (is (= "/some/:param" (common/path sim)))))))

(deftest ^:unit ->WsSimulator.reset-test
  (testing "(->WsSimulator.reset)"
    (testing "resets the simulator"
      (let [[sim _ dispatch] (simulator)]
        (common/reset! sim)
        (is (spies/called-with? dispatch actions/disconnect-all))
        (is (spies/called-with? dispatch actions/reset))))))

(deftest ^:unit ->WsSimulator.routes-test
  (testing "(->WsSimulator.routes)"
    (with-redefs [routes.sim/ws-sim->routes (spies/constantly ::routes)]
      (testing "returns routes"
        (let [[sim] (simulator)
              result (common/routes sim)]
          (is (spies/called-with? routes.sim/ws-sim->routes ::env sim))
          (is (= ::routes result)))))))

(deftest ^:unit ->WsSimulator.change-test
  (testing "(->WsSimulator.change)"
    (let [[sim _ dispatch] (simulator)
          config {:delay 100 :response {:body "{\"some\":\"json\"}"} :extra ::junk}]
      (with-redefs [actions/change (spies/constantly ::action)]
        (testing "changes changeable config properties"
          (common/reset! sim (assoc config :method ::method :path ::path))
          (is (spies/called-with? actions/change config))
          (is (spies/called-with? dispatch ::action)))

        (testing "and when the config is invalid"
          (is (thrown? Throwable (common/reset! sim ::invalid))))))))

(deftest ^:unit ->WsSimulator.reset-messages-test
  (testing "(->WsSimulator.reset-messages)"
    (let [[sim _ dispatch] (simulator)]
      (common/partially-reset! sim :ws/requests)
      (is (spies/called-with? dispatch actions/reset-messages)))))

(deftest ^:unit ->WsSimulator.connect-test
  (testing "(->WsSimulator.connect)"
    (with-redefs [web.async/as-channel (spies/constantly ::chan)
                  ws.sim/on-open (spies/create)
                  ws.sim/on-message (spies/create)
                  ws.sim/on-close (spies/create)]
      (let [[sim _ dispatch get-state] (simulator)
            request {::some ::details :websocket? true}
            store {:dispatch dispatch :get-state get-state}]
        (testing "when the request is for a websocket"
          (let [result (common/connect! sim request)
                [_ {:keys [on-open on-message on-close]}] (first (spies/calls web.async/as-channel))]
            (testing "builds the channel"
              (is (spies/called-with? web.async/as-channel request (spies/matcher map?))))

            (testing "handles :on-open"
              (on-open ::ws)
              (is (spies/called-with? ws.sim/on-open ::env sim request store ::ws)))

            (testing "handles :on-message"
              (on-message ::ws ::message)
              (is (spies/called-with? ws.sim/on-message sim request store ::ws ::message)))

            (testing "handles :on-close"
              (on-close ::ws ::reason)
              (is (spies/called-with? ws.sim/on-close ::env sim request store ::ws ::reason)))

            (testing "returns the channel"
              (is (= ::chan result)))))

        (testing "when the request is not for a websocket"
          (spies/reset! web.async/as-channel)
          (let [result (common/connect! sim (assoc request :websocket? false))]
            (testing "does not build a channel"
              (is (spies/never-called? web.async/as-channel)))

            (testing "returns nil"
              (is (nil? result)))))))))

(deftest ^:unit ->WsSimulator.disconnect-test
  (testing "(->WsSimulator.disconnect)"
    (testing "when called without a socket-id"
      (let [[sim _ dispatch] (simulator)]
        (common/disconnect! sim)
        (testing "disconnects all sockets"
          (is (spies/called-with? dispatch actions/disconnect-all)))))

    (testing "when called with a socket-id"
      (with-redefs [actions/disconnect (spies/constantly ::action)]
        (let [[sim _ dispatch] (simulator)]
          (common/disconnect! sim ::socket-id)
          (testing "disconnects a specific socket"
            (is (spies/called-with? actions/disconnect ::socket-id))
            (is (spies/called-with? dispatch ::action))))))))

(deftest ^:unit ->WsSimulator.send-test
  (testing "(->WsSimulator.send)"
    (testing "when called without a socket-id"
      (with-redefs [actions/send-all (spies/constantly ::action)]
        (let [[sim _ dispatch] (simulator)]
          (common/send! sim ::message)
          (testing "sends a specific socket"
            (is (spies/called-with? actions/send-all ::message))
            (is (spies/called-with? dispatch ::action))))))

    (testing "when called with a socket-id"
      (with-redefs [actions/send-one (spies/constantly ::action)]
        (let [[sim _ dispatch] (simulator)]
          (common/send! sim ::socket-id ::message)
          (testing "sends a specific socket"
            (is (spies/called-with? actions/send-one ::socket-id ::message))
            (is (spies/called-with? dispatch ::action))))))))

(deftest ^:unit ->WsSimulator.equals-test
  (testing "(->WsSimulator.equals"
    (testing "when the path sections match"
      (with-redefs [nav*/path-matcher (constantly (constantly true))]
        (are [config-1 config-2 does?] (let [[sim-1] (simulator config-1)
                                             sim-2 (when (map? config-2)
                                                     (reify common/IIdentify
                                                       (method [_]
                                                         (:method config-2))
                                                       (path [_]
                                                         (:path config-2))))]
                                         (= does? (= sim-1 sim-2)))
          {:method :ws/ws :path "/path"} {:method :ws :path ::path} true
          {:method :ws/ws :path "/path"} {:method :delete :path ::path} false
          {:method :ws/ws :path "/path"} #{} false
          {:method :ws/ws :path "/path"} "" false
          {:method :ws/ws :path "/path"} nil false)))

    (testing "when the path sections do not match"
      (with-redefs [nav*/path-matcher (constantly (constantly false))]
        (let [[sim-1] (simulator {:method :ws/ws :path "/path"})
              sim-2 (reify common/IIdentify
                      (method [_]
                        (:method :ws))
                      (path [_]
                        (:path ::path)))]
          (is (not= sim-1 sim-2)))))))
