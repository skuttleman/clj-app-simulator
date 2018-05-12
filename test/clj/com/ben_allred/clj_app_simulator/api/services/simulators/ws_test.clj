(ns com.ben-allred.clj-app-simulator.api.services.simulators.ws-test
  (:require [clojure.test :refer [deftest testing is are]]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.api.services.simulators.store.core :as store]
            [com.ben-allred.clj-app-simulator.api.services.simulators.ws :as ws.sim]
            [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
            [com.ben-allred.clj-app-simulator.api.services.simulators.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.api.services.activity :as activity]
            [com.ben-allred.clj-app-simulator.utils.uuids :as uuids]
            [com.ben-allred.clj-app-simulator.api.services.simulators.routes :as routes.sim]
            [immutant.web.async :as web.async]))

(defn ^:private simulator
  ([] (simulator {:method :ws
                  :path   "/some/path"}))
  ([config]
   (let [dispatch (spies/create)
         get-state (spies/create (constantly ::state))
         spy (spies/create (constantly {:dispatch  dispatch
                                        :get-state get-state}))]
     (with-redefs [store/ws-store spy]
       [(ws.sim/->WsSimulator ::id config) spy config dispatch get-state]))))

(deftest ^:unit valid?-test
  (testing "(valid?)"
    (testing "recognizes valid configs"
      (are [config] (ws.sim/valid? config)
        {:path "/some/path" :method :ws}
        {:path "/" :method "ws"}
        {:path "/some/path" :method :ws}
        {:path "/this/is/also/valid" :method "ws"}))

    (testing "recognizes invalid configs"
      (are [config] (not (ws.sim/valid? config))
        {}
        {:path "/valid/path"}
        {:method :ws}
        {:path nil :method nil}
        {:path "" :method :method}
        {:path "/$$$" :method "http/get"}
        {:path ::path :method ::method}))))

(deftest ^:unit on-open-test
  (testing "(on-open)"
    (let [dispatch-spy (spies/create)
          actions-spy (spies/create (constantly ::action))
          publish-spy (spies/create)
          details-spy (spies/create (constantly {::some ::details}))
          uuid (uuids/random)]
      (with-redefs [actions/connect actions-spy
                    activity/publish publish-spy
                    common/details details-spy
                    uuids/random (constantly uuid)]
        (testing "when the connection is opened"
          (ws.sim/on-open ::simulator ::request {:dispatch dispatch-spy} ::ws)
          (testing "dispatches an action"
            (is (spies/called-with? actions-spy uuid ::ws))
            (is (spies/called-with? dispatch-spy ::action)))

          (testing "publishes an event"
            (is (spies/called-with? details-spy ::simulator))
            (is (spies/called-with? publish-spy :ws/connect {::some ::details :socket-id uuid}))))))))

(deftest ^:unit on-message-test
  (testing "(on-message)"
    (let [find-socket-spy (spies/create (constantly ::socket-id))
          receive-spy (spies/create)
          get-state-spy (spies/create (constantly ::state))]
      (with-redefs [actions/find-socket-id find-socket-spy
                    common/receive receive-spy]
        (testing "when the socket-id is found"
          (spies/reset! find-socket-spy receive-spy get-state-spy)
          (ws.sim/on-message ::simulator
                             {:query-params ::query-params :route-params ::route-params :headers ::headers}
                             {:get-state get-state-spy}
                             ::ws
                             ::message)
          (testing "receives the request"
            (is (spies/called? get-state-spy))
            (is (spies/called-with? find-socket-spy ::state ::ws))
            (is (spies/called-with? receive-spy
                                    ::simulator
                                    {:headers      ::headers
                                     :query-params ::query-params
                                     :route-params ::route-params
                                     :socket-id    ::socket-id
                                     :body         ::message}))))

        (testing "when the socket-id is not found"
          (spies/reset! find-socket-spy receive-spy get-state-spy)
          (spies/respond-with! find-socket-spy (constantly nil))
          (testing "does not receive the request"
            (spies/never-called? receive-spy)))))))

(deftest ^:unit on-close-test
  (testing "(on-close)"
    (let [find-socket-spy (spies/create (constantly ::socket-id))
          action-spy (spies/create (constantly ::action))
          get-state-spy (spies/create (constantly ::state))
          dispatch-spy (spies/create)
          publish-spy (spies/create)
          details-spy (spies/create (constantly {::some ::details}))]
      (with-redefs [actions/find-socket-id find-socket-spy
                    actions/remove-socket action-spy
                    activity/publish publish-spy
                    common/details details-spy]
        (testing "when the socket-id is found"
          (spies/reset! find-socket-spy get-state-spy action-spy dispatch-spy publish-spy details-spy)
          (ws.sim/on-close ::simulator
                           ::request
                           {:dispatch dispatch-spy :get-state get-state-spy}
                           ::ws
                           ::reason)

          (testing "dispatches an event"
            (is (spies/called-with? find-socket-spy ::state ::ws))
            (is (spies/called-with? action-spy ::socket-id))
            (is (spies/called-with? dispatch-spy ::action)))

          (testing "publishes an event"
            (is (spies/called-with? details-spy ::simulator))
            (is (spies/called-with? publish-spy
                                    :ws/disconnect
                                    {::some ::details :socket-id ::socket-id}))))

        (testing "when the socket-id is not found"
          (spies/reset! find-socket-spy get-state-spy action-spy dispatch-spy publish-spy details-spy)
          (spies/respond-with! find-socket-spy (constantly nil))
          (testing "does not dispatch an event"
            (is (spies/never-called? action-spy))
            (is (spies/never-called? dispatch-spy)))

          (testing "does not publish an event"
            (is (spies/never-called? publish-spy))))))))

(deftest ^:unit ->WsSimulator-test
  (testing "(->WsSimulator)"
    (let [action-spy (spies/create (constantly ::action))]
      (with-redefs [actions/init action-spy]
        (testing "when the config is valid"
          (let [[sim _ config dispatch] (simulator)]
            (testing "initializes the store"
              (is (spies/called-with? action-spy config))
              (is (spies/called-with? dispatch ::action)))

            (testing "returns a simulator"
              (is (satisfies? common/ISimulator sim))
              (is (satisfies? common/IWSSimulator sim)))))

        (testing "when the config is invalid"
          (let [[sim _ _ dispatch] (simulator {})]
            (testing "does not initialize the store"
              (is (spies/never-called? dispatch)))

            (testing "returns nil"
              (is (nil? sim)))))))))

(deftest ^:unit ->WsSimulator.start-test
  (testing "(->WsSimulator.start)"
    (let [[sim] (simulator)]
      (testing "does not expload"
        (common/start sim)))))

(deftest ^:unit ->WsSimulator.stop-test
  (testing "(->WsSimulator.stop)"
    (let [[sim _ _ dispatch] (simulator)]
      (common/stop sim)
      (is (spies/called-with? dispatch actions/disconnect-all)))))

(deftest ^:unit ->WsSimulator.receive-test
  (testing "(->WsSimulator.receive)"
    (let [action-spy (spies/create (constantly ::action))
          receive-spy (spies/create)]
      (with-redefs [actions/receive action-spy
                    routes.sim/receive receive-spy]
        (testing "receives messages"
          (let [[sim _ _ dispatch] (simulator)
                request {::some ::things :socket-id ::socket-id}]
            (common/receive sim request)
            (is (spies/called-with? action-spy request))
            (is (spies/called-with? dispatch ::action))
            (is (spies/called-with? receive-spy sim {:socket-id ::socket-id}))))))))

(deftest ^:unit ->WsSimulator.requests-test
  (testing "(->WsSimulator.requests)"
    (let [requests-spy (spies/create (constantly ::requests))]
      (with-redefs [store/requests requests-spy]
        (testing "returns requests"
          (let [[sim _ _ _ get-state] (simulator)
                result (common/requests sim)]
            (is (spies/called-with? get-state))
            (is (spies/called-with? requests-spy ::state))
            (is (= ::requests result))))))))

(deftest ^:unit ->WsSimulator.details-test
  (testing "(->WsSimulator.details)"
    (let [details-spy (spies/create (constantly {::some ::details}))]
      (with-redefs [store/details details-spy]
        (testing "returns details"
          (let [[sim _ _ _ get-state] (simulator)
                result (common/details sim)]
            (is (spies/called-with? get-state))
            (is (spies/called-with? details-spy ::state))
            (is (= {::some ::details :id ::id} result))))))))

(deftest ^:unit ->WsSimulator.reset-test
  (testing "(->WsSimulator.reset)"
    (testing "resets the simulator"
      (let [[sim _ _ dispatch] (simulator)]
        (common/reset sim)
        (is (spies/called-with? dispatch actions/reset))))))

(deftest ^:unit ->WsSimulator.routes-test
  (testing "(->WsSimulator.routes)"
    (let [routes-spy (spies/create (constantly ::routes))]
      (with-redefs [routes.sim/ws-sim->routes routes-spy]
        (testing "returns routes"
          (let [[sim] (simulator)
                result (common/routes sim)]
            (is (spies/called-with? routes-spy sim))
            (is (= ::routes result))))))))

(deftest ^:unit ->WsSimulator.connect-test
  (testing "(->WsSimulator.connect)"
    (let [channel-spy (spies/create (constantly ::chan))
          open-spy (spies/create)
          message-spy (spies/create)
          close-spy (spies/create)]
      (with-redefs [web.async/as-channel channel-spy
                    ws.sim/on-open open-spy
                    ws.sim/on-message message-spy
                    ws.sim/on-close close-spy]
        (let [[sim _ _ dispatch get-state] (simulator)
              request {::some ::details :websocket? true}
              store {:dispatch dispatch :get-state get-state}]
          (testing "when the request is for a websocket"
            (let [result (common/connect sim request)
                  [_ {:keys [on-open on-message on-close]}] (first (spies/calls channel-spy))]
              (testing "builds the channel"
                (is (spies/called-with? channel-spy request (spies/matcher map?))))

              (testing "handles :on-open"
                (on-open ::ws)
                (is (spies/called-with? open-spy sim request store ::ws)))

              (testing "handles :on-message"
                (on-message ::ws ::message)
                (is (spies/called-with? message-spy sim request store ::ws ::message)))

              (testing "handles :on-close"
                (on-close ::ws ::reason)
                (is (spies/called-with? close-spy sim request store ::ws ::reason)))

              (testing "returns the channel"
                (is (= ::chan result)))))

          (testing "when the request is not for a websocket"
            (spies/reset! channel-spy)
            (let [result (common/connect sim (assoc request :websocket? false))]
              (testing "does not build a channel"
                (is (spies/never-called? channel-spy)))

              (testing "returns nil"
                (is (nil? result))))))))))

(deftest ^:unit ->WsSimulator.disconnect-test
  (testing "(->WsSimulator.disconnect)"
    (testing "when called without a socket-id"
      (let [[sim _ _ dispatch] (simulator)]
        (common/disconnect sim)
        (testing "disconnects all sockets"
          (is (spies/called-with? dispatch actions/disconnect-all)))))

    (testing "when called with a socket-id"
      (let [action-spy (spies/create (constantly ::action))]
        (with-redefs [actions/disconnect action-spy]
          (let [[sim _ _ dispatch] (simulator)]
            (common/disconnect sim ::socket-id)
            (testing "disconnects a specific socket"
              (is (spies/called-with? action-spy ::socket-id))
              (is (spies/called-with? dispatch ::action)))))))))

(deftest ^:unit ->WsSimulator.send-test
  (testing "(->WsSimulator.send)"
    (testing "when called without a socket-id"
      (let [action-spy (spies/create (constantly ::action))]
        (with-redefs [actions/send-all action-spy]
          (let [[sim _ _ dispatch] (simulator)]
            (common/send sim ::message)
            (testing "sends a specific socket"
              (is (spies/called-with? action-spy ::message))
              (is (spies/called-with? dispatch ::action)))))))

    (testing "when called with a socket-id"
      (let [action-spy (spies/create (constantly ::action))]
        (with-redefs [actions/send-one action-spy]
          (let [[sim _ _ dispatch] (simulator)]
            (common/send sim ::socket-id ::message)
            (testing "sends a specific socket"
              (is (spies/called-with? action-spy ::socket-id ::message))
              (is (spies/called-with? dispatch ::action)))))))))
