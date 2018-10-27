(ns integration.tests.simulators.ws
  (:require
    [clojure.core.async :as async]
    [clojure.edn :as edn]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [com.ben-allred.clj-app-simulator.utils.json :as json]
    [com.ben-allred.clj-app-simulator.utils.logging :as log]
    [com.ben-allred.clj-app-simulator.utils.transit :as transit]
    [integration.utils.chans :as chans]
    [integration.utils.fixtures :as fixtures]
    [integration.utils.http :as test.http]
    [integration.utils.ws :as test.ws]))

(use-fixtures :once fixtures/run-server)

(def ^:private content-type->parser
  {"application/edn"     edn/read-string
   "application/transit" transit/parse
   "application/json"    json/parse})

(def ^:private start-configs [{:method :ws/ws
                               :path   "/some/:url-param"
                               :name   "Paramy"}
                              {:method :ws/ws
                               :path   "/some/other/path"
                               :name   "Othero"}])

(def ^:private new-configs [{:method :ws/ws
                             :path   "/something/new"
                             :name   "Freshy"}
                            {:method :ws/ws
                             :path   "/"
                             :name   "Rooty"}])

(defn ^:private sims-match? [sims configs]
  (= (set (map (comp #(update % :method keyword) :config) sims))
     (set configs)))

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
              (let [{:keys [event data]} (chans/<⏰!! chan)]
                (is (= :simulators/init (keyword event)))
                (is (sims-match? (:simulators data) start-configs))))

            (testing "and when getting a list of simulators"
              (let [sims (-> (test.http/get "/api/simulators" content-type)
                             (second)
                             (:simulators))]
                (testing "returns the simulators"
                  (is (sims-match? sims start-configs)))))

            (testing "and when adding a simulator"
              (let [simulator {:path "/new/sim" :method :ws/ws :name "Newman"}
                    response (test.http/post "/api/simulators" content-type {:body {:simulator simulator}})]
                (testing "returns a success"
                  (is (test.http/success? response)))

                (testing "publishes an event"
                  (let [{:keys [event data]} (chans/<⏰!! chan)]
                    (is (= :simulators/add (keyword event)))
                    (is (sims-match? [(:simulator data)] [simulator]))))

                (testing "and when getting a list of simulators"
                  (let [sims (-> (test.http/get "/api/simulators" content-type)
                                 (second)
                                 (:simulators))]
                    (testing "includes the new simulator"
                      (is (has-sim? sims simulator)))))))

            (testing "and when adding a simulator that exists"
              (let [simulator {:method :ws/ws :path "/some/:url-param" :name "Dupy"}
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
              (let [[_ body :as response] (test.http/post "/api/simulators/init" content-type {:body {:simulators new-configs}})
                    sim->id (into {}
                                  (map (juxt (comp :path :config) :id))
                                  (:simulators body))]
                (testing "returns a success"
                  (is (test.http/success? response)))

                (testing "publishes an event"
                  (let [{:keys [event data]} (chans/<⏰!! chan)]
                    (is (= :simulators/init (keyword event)))
                    (is (sims-match? (:simulators data) new-configs))))

                (testing "and when getting a list of simulators"
                  (let [sims (-> (test.http/get "/api/simulators" content-type)
                                 (second)
                                 (:simulators))]
                    (testing "only has the new simulators"
                      (is (sims-match? sims new-configs)))))

                (testing "and when connecting sockets to a simulator"
                  (let [ws-1 (test.ws/connect "/simulators")
                        ws-2 (test.ws/connect "/simulators")
                        ws-3 (test.ws/connect "/simulators/something/new")]
                    (chans/flush! chan)
                    (testing "and when deleting a simulator"
                      (let [simulator {:method :ws/ws :path "/" :name "Rooty"}
                            response (test.http/delete (str "/api/simulators/" (sim->id "/")) content-type)]
                        (testing "returns a success"
                          (is (test.http/success? response)))

                        (testing "publishes an event"
                          (let [{:keys [event data]} (chans/<⏰!! chan)]
                            (is (= :simulators/delete (keyword event)))
                            (is (sims-match? [(:simulator data)] [simulator]))))

                        (testing "when disconnecting sockets"
                          (is (thrown? Throwable (test.ws/send! ws-1 "message")))
                          (is (thrown? Throwable (test.ws/send! ws-2 "message")))
                          (test.ws/send! ws-3 "message")

                          (testing "publishes an event for ws-1"
                            (let [{:keys [event data]} (chans/<⏰!! chan)]
                              (is (= :simulators.ws/disconnect (keyword event)))
                              (is (not (contains? (set (:sockets data)) (:socket-id data))))))

                          (testing "publishes an event for ws-2"
                            (let [{:keys [event data]} (chans/<⏰!! chan)]
                              (is (= :simulators.ws/disconnect (keyword event)))
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
        (let [[_ body :as response] (test.http/post "/api/simulators/init" content-type {:body {:simulators start-configs}})
              sim->id (into {}
                            (map (juxt (comp :path :config) :id))
                            (:simulators body))]
          (is (test.http/success? response))
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
                    {:keys [event data]} (chans/<⏰!! chan)
                    socket-id-1 (:socket-id data)]
                (testing "publishes an event"
                  (is (= :simulators.ws/connect (keyword event)))
                  (is socket-id-1))

                (testing "and when connecting a second socket"
                  (let [chan-2 (async/chan 64)
                        ws-2 (test.ws/connect "/simulators/some/:url-param"
                                              :on-msg (partial async/put! chan-2)
                                              :to-clj edn/read-string)
                        {:keys [event data]} (chans/<⏰!! chan)
                        socket-id-2 (:socket-id data)]
                    (testing "publishes and event"
                      (is (= :simulators.ws/connect (keyword event)))
                      (is socket-id-2))

                    (testing "and when getting the simulator's details"
                      (let [sim (-> (test.http/get (str "/api/simulators/" (sim->id "/some/:url-param")) content-type)
                                    (second)
                                    (:simulator))
                            sockets (set (:sockets sim))]
                        (testing "includes both socket ids"
                          (is (contains? sockets socket-id-1))
                          (is (contains? sockets socket-id-2)))))

                    (testing "and when when sending a message via socket"
                      (test.ws/send! ws-1 "this is a message")
                      (testing "publishes an event"
                        (let [{:keys [event data]} (chans/<⏰!! chan)]
                          (is (= socket-id-1 (:socket-id (:request data))))
                          (is (= "this is a message") (:body data))
                          (is (= :simulators/receive (keyword event)))))

                      (testing "and when getting the simulator's details"
                        (let [request (-> (test.http/get (str "/api/simulators/" (sim->id "/some/:url-param")) content-type)
                                          (second)
                                          (:simulator)
                                          (:requests)
                                          (first))]
                          (testing "includes the socket message with the socket id"
                            (is (= socket-id-1 (:socket-id request)))
                            (is (= "this is a message" (:body request)))))))

                    (testing "and when posting a message to the first socket"
                      (let [response (test.http/post (str "/api/simulators/" (sim->id "/some/:url-param") "/sockets/" socket-id-1)
                                                     "application/edn"
                                                     {:body {:some [:edn :data]}})]
                        (testing "returns a success"
                          (is (test.http/success? response)))

                        (testing "receives the message"
                          (let [message (chans/<⏰!! chan-1)]
                            (is (= {:some [:edn :data]} message))))))

                    (testing "and when posting a message to all sockets"
                      (let [response (test.http/post (str "/api/simulators/" (sim->id "/some/:url-param"))
                                                     "application/edn"
                                                     {:body {:broadcast [:4 :all]}})]
                        (testing "returns a success"
                          (is (test.http/success? response)))

                        (testing "receives the message for socket-1"
                          (let [message (chans/<⏰!! chan-1)]
                            (is (= {:broadcast [:4 :all]} message))))

                        (testing "receives the message for socket-2"
                          (let [message (chans/<⏰!! chan-2)]
                            (is (= {:broadcast [:4 :all]} message))))))

                    (testing "and when disconnecting the first socket"
                      (test.ws/close! ws-1)
                      (testing "publishes an event"
                        (let [{:keys [event data]} (chans/<⏰!! chan)]
                          (is (= :simulators.ws/disconnect (keyword event)))
                          (is (not (contains? (set (:sockets data)) socket-id-1)))))

                      (testing "and when sending a request to disconnect the second socket"
                        (let [response (test.http/patch (str "/api/simulators/" (sim->id "/some/:url-param"))
                                                        content-type
                                                        {:body {:action :simulators.ws/disconnect :socket-id socket-id-2}})]
                          (testing "returns a success"
                            (is (test.http/success? response)))

                          (testing "publishes an event"
                            (let [{:keys [event data]} (chans/<⏰!! chan)]
                              (is (= :simulators.ws/disconnect (keyword event)))
                              (is (not (contains? (set (:sockets data)) socket-id-2)))))

                          (testing "disconnects the socket"
                            (is (thrown? Throwable (test.ws/send! ws-2 "a message")))))

                        (testing "and when getting the simulator's details"
                          (let [sockets (-> (test.http/get (str "/api/simulators/" (sim->id "/some/:url-param")) content-type)
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
                    {event-1 :event data-1 :data} (chans/<⏰!! chan)
                    socket-id-1 (:socket-id data-1)
                    chan-2 (async/chan 64)
                    ws-2 (test.ws/connect "/simulators/some/:url-param"
                                          :on-msg (partial async/put! chan-2)
                                          :to-clj edn/read-string)
                    {event-2 :event data-2 :data} (chans/<⏰!! chan)
                    socket-id-2 (:socket-id data-2)]
                (testing "publishes an event for the first socket"
                  (is (= :simulators.ws/connect (keyword event-1))))

                (testing "publishes an event for the second socket"
                  (is (= :simulators.ws/connect (keyword event-2))))

                (testing "and when sending a message from a socket"
                  (test.ws/send! ws-1 "a message")
                  (testing "publishes an event"
                    (let [{:keys [event]} (chans/<⏰!! chan)]
                      (is (= :simulators/receive (keyword event)))))

                  (testing "and when resetting the simulator's messages"
                    (let [response (test.http/patch (str "/api/simulators/" (sim->id "/some/:url-param"))
                                                    content-type
                                                    {:body {:action :simulators/reset :type :ws/requests}})]
                      (testing "returns a success"
                        (is (test.http/success? response)))

                      (testing "publishes an event"
                        (let [{:keys [event data]} (chans/<⏰!! chan)]
                          (is (= :simulators/reset (keyword event)))
                          (is (empty? (:requests data)))))

                      (testing "and when getting the simulator's details"
                        (let [requests (-> (test.http/get (str "/api/simulators/" (sim->id "/some/:url-param")) content-type)
                                           (second)
                                           (:simulator)
                                           (:requests))]
                          (testing "has no messages"
                            (is (empty? requests)))))))

                  (testing "and when disconnecting all sockets"
                    (let [response (test.http/patch (str "/api/simulators/" (sim->id "/some/:url-param"))
                                                    content-type
                                                    {:body {:action :simulators.ws/disconnect}})]
                      (testing "returns a success"
                        (is (test.http/success? response)))

                      (let [{event-1 :event data-1 :data} (chans/<⏰!! chan)
                            {event-2 :event data-2 :data} (chans/<⏰!! chan)
                            sockets #{(:socket-id data-1) (:socket-id data-2)}]
                        (testing "publishes an event for the first socket"
                          (is (= :simulators.ws/disconnect (keyword event-1)))
                          (is (contains? sockets socket-id-1)))

                        (testing "publishes an event for the second socket"
                          (is (= :simulators.ws/disconnect (keyword event-2)))
                          (is (contains? sockets socket-id-2))))

                      (testing "and when getting the simulator's details"
                        (let [sockets (-> (test.http/get (str "/api/simulators/" (sim->id "/some/:url-param")) content-type)
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
                    {event-1 :event data-1 :data} (chans/<⏰!! chan)
                    socket-id-1 (:socket-id data-1)
                    chan-2 (async/chan 64)
                    ws-2 (test.ws/connect "/simulators/some/:url-param"
                                          :on-msg (partial async/put! chan-2)
                                          :to-clj edn/read-string)
                    {event-2 :event data-2 :data} (chans/<⏰!! chan)
                    socket-id-2 (:socket-id data-2)]
                (testing "publishes an event for the first socket"
                  (is (= :simulators.ws/connect (keyword event-1))))

                (testing "publishes an event for the second socket"
                  (is (= :simulators.ws/connect (keyword event-2))))

                (testing "and when resetting the simulator"
                  (let [response (test.http/patch (str "/api/simulators/" (sim->id "/some/:url-param"))
                                                  content-type
                                                  {:body {:action :simulators/reset}})]
                    (testing "returns a success"
                      (is (test.http/success? response)))

                    (testing "publishes an event for each socket that was disconnected"
                      (let [{event-1 :event data-1 :data} (chans/<⏰!! chan)
                            {event-2 :event data-2 :data} (chans/<⏰!! chan)
                            sockets #{(:socket-id data-1) (:socket-id data-2)}]
                        (is (= :simulators.ws/disconnect (keyword event-1)))
                        (is (= :simulators.ws/disconnect (keyword event-2)))
                        (is (contains? sockets socket-id-1))
                        (is (contains? sockets socket-id-2))))

                    (testing "publishes an event"
                      (let [{:keys [event data]} (chans/<⏰!! chan)]
                        (is (= :simulators/reset (keyword event)))
                        (is (sims-match? [(:simulator data)] [{:path "/some/:url-param" :method :ws/ws :name "Paramy"}]))))

                    (testing "and when getting the simulator's details"
                      (let [sim (-> (test.http/get (str "/api/simulators/" (sim->id "/some/:url-param")) content-type)
                                    (second)
                                    (:simulator))]
                        (testing "has no messages"
                          (is (empty? (:requests sim))))

                        (testing "has no connections"
                          (is (empty? (:sockets sim))))))))

                (test.ws/close! ws-1)
                (async/close! chan-1)
                (test.ws/close! ws-2)
                (async/close! chan-2)))

            (test.ws/close! ws)
            (async/close! chan)))))))
