(ns integration.tests.simulators.ws
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [integration.utils.fixtures :as fixtures]
            [clojure.edn :as edn]
            [com.ben-allred.clj-app-simulator.utils.transit :as transit]
            [com.ben-allred.clj-app-simulator.utils.json :as json]
            [clojure.core.async :as async]
            [integration.utils.ws :as test.ws]
            [integration.utils.http :as test.http]
            [integration.utils.chans :as chans]))

(use-fixtures :once fixtures/run-server)

(def ^:private content-type->parser
  {"application/edn"     edn/read-string
   "application/transit" transit/parse
   "application/json"    json/parse})

(def ^:private start-configs [{:method :ws
                               :path   "/some/:url-param"
                               :name   "Paramy"}
                              {:method :ws
                               :path   "/some/other/path"
                               :name   "Othero"}])

(def ^:private new-configs [{:method :ws
                             :path   "/something/new"
                             :name   "Freshy"}
                            {:method :ws
                             :path   "/"
                             :name   "Rooty"}])

(defn ^:private sims-match? [sims configs]
  (= (map (comp #(update % :method keyword) :config) sims)
     configs))

(defn ^:private has-sim? [sims config]
  (some (fn [sim] (sims-match? [sim] [config])) sims))

(deftest ^:integration ws-simulators-init-test
  (testing "[simulators API]"
    (doseq [content-type ["application/edn" "application/transit" "application/json"]]
      (let [chan (async/chan 64)
            ws (test.ws/connect "/api/simulators/activity"
                                :query-params {:accept content-type}
                                :on-msg (partial async/put! chan)
                                :to-clj (content-type->parser content-type))]
        (testing "when initializing the simulators"
          (let [response (test.http/post "/api/simulators/init" content-type {:body {:simulators start-configs}})]
            (testing "returns a success"
              (is (test.http/success? response)))

            (testing "publishes an event"
              (let [{:keys [event data]} (async/<!! chan)]
                (is (= :simulators/init (keyword event)))
                (is (sims-match? data start-configs))))

            (testing "and when getting a list of simulators"
              (let [sims (-> (test.http/get "/api/simulators" content-type)
                             (second)
                             (:simulators))]
                (testing "returns the simulators"
                  (is (sims-match? sims start-configs)))))

            (testing "and when adding a simulator"
              (let [simulator {:path "/new/sim" :method :ws :name "Newman"}
                    response (test.http/post "/api/simulators" content-type {:body {:simulator simulator}})]
                (testing "returns a success"
                  (is (test.http/success? response)))

                (testing "publishes an event"
                  (let [{:keys [event data]} (async/<!! chan)]
                    (is (= :simulators/add (keyword event)))
                    (is (sims-match? [data] [simulator]))))

                (testing "and when getting a list of simulators"
                  (let [sims (-> (test.http/get "/api/simulators" content-type)
                                 (second)
                                 (:simulators))]
                    (testing "includes the new simulator"
                      (is (has-sim? sims simulator)))))))

            (testing "and when adding a simulator that exists"
              (let [simulator {:method :ws :path "/some/:url-param" :name "Dupy"}
                    response (test.http/post "/api/simulators" content-type {:body {:simulator simulator}})]
                (testing "returns an error"
                  (is (test.http/client-error? response)))

                (testing "and when getting a list of simulators"
                  (let [sims (-> (test.http/get "/api/simulators" content-type)
                                 (second)
                                 (:simulators))]
                    (testing "does not include the bad simulator"
                      (is (not (has-sim? sims simulator))))))))

            (testing "and when re-initializing the simulators"
              (let [response (test.http/post "/api/simulators/init" content-type {:body {:simulators new-configs}})]
                (testing "returns a success"
                  (is (test.http/success? response)))

                (testing "publishes an event"
                  (let [{:keys [event data]} (async/<!! chan)]
                    (is (= :simulators/init (keyword event)))
                    (is (sims-match? data new-configs))))

                (testing "and when getting a list of simulators"
                  (let [sims (-> (test.http/get "/api/simulators" content-type)
                                 (second)
                                 (:simulators))]
                    (testing "only has the new simulators"
                      (is (sims-match? sims new-configs)))))

                (testing "and when connecting sockets to a simulator"
                  (let [ws-1 (test.ws/connect "/simulators")
                        _ (async/<!! chan)
                        ws-2 (test.ws/connect "/simulators")
                        _ (async/<!! chan)
                        ws-3 (test.ws/connect "/simulators/something/new")
                        _ (async/<!! chan)]

                    (testing "and when deleting a simulator"
                      (let [simulator {:method :ws :path "/" :name "Rooty"}
                            response (test.http/delete "/api/simulators/ws" content-type)]
                        (testing "returns a success"
                          (is (test.http/success? response)))

                        (testing "publishes an event"
                          (let [{:keys [event data]} (async/<!! chan)]
                            (is (= :simulators/delete (keyword event)))
                            (is (sims-match? [data] [simulator]))))

                        (testing "when disconnecting sockets"
                          (is (thrown? Throwable (test.ws/send! ws-1 "message")))
                          (is (thrown? Throwable (test.ws/send! ws-2 "message")))
                          (test.ws/send! ws-3 "message")

                          (testing "publishes an event for ws-1"
                            (let [{:keys [event data]} (async/<!! chan)]
                              (is (= :ws/disconnect (keyword event)))
                              (is (not (contains? (set (:sockets data)) (:socket-id data))))))

                          (testing "publishes an event for ws-2"
                            (let [{:keys [event data]} (async/<!! chan)]
                              (is (= :ws/disconnect (keyword event)))
                              (is (not (contains? (set (:sockets data)) (:socket-id data)))))))

                        (testing "and when getting a list of simulators"
                          (let [sims (-> (test.http/get "/api/simulators" content-type)
                                         (second)
                                         (:simulators))]
                            (testing "does not include the deleted simulator"
                              (is (not (has-sim? sims simulator))))))))

                    (test.ws/close! ws-1)
                    (test.ws/close! ws-2)
                    (test.ws/close! ws-3)))))))

        (test.ws/close! ws)
        (async/close! chan)))))

(deftest ^:integration ws-simulator-test
  (testing "[simulator API]"
    (doseq [content-type ["application/edn" "application/transit" "application/json"]]
      (testing "with simulators initialized"
        (let [response (test.http/post "/api/simulators/init" content-type {:body {:simulators start-configs}})]
          (is (test.http/success? response)))
        (let [chan (async/chan 64)
              ws (test.ws/connect "/api/simulators/activity"
                                  :query-params {:accept content-type}
                                  :on-msg (partial async/put! chan)
                                  :to-clj (content-type->parser content-type))]
          (testing "when connecting to a socket simulator"
            (let [chan-1 (async/chan 64)
                  ws-1 (test.ws/connect "/simulators/some/:url-param"
                                        :on-msg (partial async/put! chan-1)
                                        :to-clj edn/read-string)
                  {:keys [event data]} (async/<!! chan)
                  socket-id-1 (:socket-id data)]
              (testing "publishes an event"
                (is (= :ws/connect (keyword event)))
                (is socket-id-1))

              (testing "and when connecting a second socket"
                (let [chan-2 (async/chan 64)
                      ws-2 (test.ws/connect "/simulators/some/:url-param"
                                            :on-msg (partial async/put! chan-2)
                                            :to-clj edn/read-string)
                      {:keys [event data]} (async/<!! chan)
                      socket-id-2 (:socket-id data)]
                  (testing "publishes and event"
                    (is (= :ws/connect (keyword event)))
                    (is socket-id-2))

                  (testing "and when getting the simulator's details"
                    (let [sim (-> (test.http/get "/api/simulators/ws/some/:url-param" content-type)
                                  (second)
                                  (:simulator))
                          sockets (set (:sockets sim))]
                      (testing "includes both socket ids"
                        (is (contains? sockets socket-id-1))
                        (is (contains? sockets socket-id-2)))))

                  (testing "and when when sending a message via socket"
                    (test.ws/send! ws-1 "this is a message")
                    (testing "publishes an event"
                      (let [{:keys [event data]} (async/<!! chan)]
                        (is (= socket-id-1 (:socket-id data)))
                        (is (= "this is a message") (:body data))
                        (is (= :simulators/receive (keyword event)))))

                    (testing "and when getting the simulator's details"
                      (let [message (-> (test.http/get "/api/simulators/ws/some/:url-param" content-type)
                                        (second)
                                        (:simulator)
                                        (:messages)
                                        (first))]
                        (testing "includes the socket message with the socket id"
                          (is (= socket-id-1 (:socket-id message)))
                          (is (= "this is a message" (:body message)))))))

                  (testing "and when posting a message to the first socket"
                    (let [response (test.http/post (str "/api/simulators/ws/some/:url-param/" socket-id-1)
                                                   "application/edn"
                                                   {:body {:some [:edn :data]}})]
                      (testing "returns a success"
                        (is (test.http/success? response)))

                      (testing "receives the message"
                        (let [message (async/<!! chan-1)]
                          (is (= {:some [:edn :data]} message))))))

                  (testing "and when posting a message to all sockets"
                    (let [response (test.http/post "/api/simulators/ws/some/:url-param"
                                                   "application/edn"
                                                   {:body {:broadcast [:4 :all]}})]
                      (testing "returns a success"
                        (is (test.http/success? response)))

                      (testing "receives the message for socket-1"
                        (let [message (async/<!! chan-1)]
                          (is (= {:broadcast [:4 :all]} message))))

                      (testing "receives the message for socket-2"
                        (let [message (async/<!! chan-2)]
                          (is (= {:broadcast [:4 :all]} message))))))

                  (testing "and when disconnecting the first socket"
                    (test.ws/close! ws-1)
                    (testing "publishes an event"
                      (let [{:keys [event data]} (async/<!! chan)]
                        (is (= :ws/disconnect (keyword event)))
                        (is (not (contains? (set (:sockets data)) socket-id-1)))))

                    (testing "and when sending a request to disconnect the second socket"
                      (let [response (test.http/patch (str "/api/simulators/ws/some/:url-param")
                                                      content-type
                                                      {:body {:action :ws/disconnect :socket-id socket-id-2}})]
                        (testing "returns a success"
                          (is (test.http/success? response)))

                        (testing "publishes an event"
                          (let [{:keys [event data]} (async/<!! chan)]
                            (is (= :ws/disconnect (keyword event)))
                            (is (not (contains? (set (:sockets data)) socket-id-2)))))

                        (testing "disconnects the socket"
                          (is (thrown? Throwable (test.ws/send! ws-2 "a message")))))

                      (testing "and when getting the simulator's details"
                        (let [sockets (-> (test.http/get "/api/simulators/ws/some/:url-param" content-type)
                                          (second)
                                          (:simulator)
                                          (:sockets))]
                          (testing "does not have any active sockets"
                            (is (empty? sockets)))))))

                  (test.ws/close! ws-2)
                  (async/close! chan-2)))
              (test.ws/close! ws-1)
              (async/close! chan-1)))

          (testing "and when connecting two new sockets"
            (chans/flush! chan)
            (let [chan-1 (async/chan 64)
                  ws-1 (test.ws/connect "/simulators/some/:url-param"
                                        :on-msg (partial async/put! chan-1)
                                        :to-clj edn/read-string)
                  {event-1 :event data-1 :data} (async/<!! chan)
                  socket-id-1 (:socket-id data-1)
                  chan-2 (async/chan 64)
                  ws-2 (test.ws/connect "/simulators/some/:url-param"
                                        :on-msg (partial async/put! chan-2)
                                        :to-clj edn/read-string)
                  {event-2 :event data-2 :data} (async/<!! chan)
                  socket-id-2 (:socket-id data-2)]
              (testing "publishes an event for the first socket"
                (is (= :ws/connect (keyword event-1))))

              (testing "publishes an event for the second socket"
                (is (= :ws/connect (keyword event-2))))

              (testing "and when sending a message from a socket"
                (test.ws/send! ws-1 "a message")
                (testing "publishes an event"
                  (let [{:keys [event]} (async/<!! chan)]
                    (is (= :simulators/receive (keyword event)))))

                (testing "and when resetting the simulator's messages"
                  (let [response (test.http/patch "/api/simulators/ws/some/:url-param"
                                                  content-type
                                                  {:body {:action :ws/reset-messages}})]
                    (testing "returns a success"
                      (is (test.http/success? response)))

                    (testing "publishes an event"
                      (let [{:keys [event data]} (async/<!! chan)]
                        (is (= :ws/reset-messages (keyword event)))
                        (is (empty? (:messages data)))))

                    (testing "and when getting the simulator's details"
                      (let [messages (-> (test.http/get "/api/simulators/ws/some/:url-param" content-type)
                                         (second)
                                         (:simulator)
                                         (:messages))]
                        (testing "has no messages"
                          (is (empty? messages)))))))

                (testing "and when disconnecting all sockets"
                  (let [response (test.http/patch "/api/simulators/ws/some/:url-param"
                                                  content-type
                                                  {:body {:action :ws/disconnect-all}})]
                    (testing "returns a success"
                      (is (test.http/success? response)))

                    (testing "publishes an event for the first socket"
                      (let [{event-1 :event data-1 :data} (async/<!! chan)
                            {event-2 :event data-2 :data} (async/<!! chan)
                            sockets #{(:socket-id data-1) (:socket-id data-2)}]
                        (is (= :ws/disconnect (keyword event-1)))
                        (is (contains? sockets socket-id-1))

                        (testing "publishes an event for the second socket"
                          (is (= :ws/disconnect (keyword event-2)))
                          (is (contains? sockets socket-id-2)))))

                    (testing "and when getting the simulator's details"
                      (let [sockets (-> (test.http/get "/api/simulators/ws/some/:url-param" content-type)
                                        (second)
                                        (:simulator)
                                        (:sockets))]
                        (testing "has no connections"
                          (is (empty? sockets))))))))

              (test.ws/close! ws-1)
              (async/close! chan-1)
              (test.ws/close! ws-2)
              (async/close! chan-2)))

          (testing "and when connecting two new sockets"
            (chans/flush! chan)
            (let [chan-1 (async/chan 64)
                  ws-1 (test.ws/connect "/simulators/some/:url-param"
                                        :on-msg (partial async/put! chan-1)
                                        :to-clj edn/read-string)
                  {event-1 :event data-1 :data} (async/<!! chan)
                  socket-id-1 (:socket-id data-1)
                  chan-2 (async/chan 64)
                  ws-2 (test.ws/connect "/simulators/some/:url-param"
                                        :on-msg (partial async/put! chan-2)
                                        :to-clj edn/read-string)
                  {event-2 :event data-2 :data} (async/<!! chan)
                  socket-id-2 (:socket-id data-2)]
              (testing "publishes an event for the first socket"
                (is (= :ws/connect (keyword event-1))))

              (testing "publishes an event for the second socket"
                (is (= :ws/connect (keyword event-1))))

              (testing "and when resetting the simulator"
                (let [response (test.http/patch (str "/api/simulators/ws/some/:url-param")
                                                content-type
                                                {:body {:action :simulators/reset}})]
                  (testing "returns a success"
                    (is (test.http/success? response)))

                  (testing "publishes an event for each socket that was disconnected"
                    (let [{event-1 :event data-1 :data} (async/<!! chan)
                          {event-2 :event data-2 :data} (async/<!! chan)
                          sockets #{(:socket-id data-1) (:socket-id data-2)}]
                      (is (= :ws/disconnect (keyword event-1)))
                      (is (= :ws/disconnect (keyword event-2)))
                      (is (contains? sockets socket-id-1))
                      (is (contains? sockets socket-id-2))))

                  (testing "publishes an event"
                    (let [{:keys [event data]} (async/<!! chan)]
                      (is (= :simulators/reset (keyword event)))
                      (is (sims-match? [data] [{:path "/some/:url-param" :method :ws :name "Paramy"}]))))

                  (testing "and when getting the simulator's details"
                    (let [sim (-> (test.http/get "/api/simulators/ws/some/:url-param" content-type)
                                  (second)
                                  (:simulator))]
                      (testing "has no messages"
                        (is (empty? (:messages sim))))

                      (testing "has no connections"
                        (is (empty? (:sockets sim))))))))

              (test.ws/close! ws-1)
              (async/close! chan-1)
              (test.ws/close! ws-2)
              (async/close! chan-2)))

          (test.ws/close! ws)
          (async/close! chan))))))
