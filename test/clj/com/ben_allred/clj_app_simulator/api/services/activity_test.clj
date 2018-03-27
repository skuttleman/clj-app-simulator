(ns com.ben-allred.clj-app-simulator.api.services.activity-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.api.services.activity :as activity]
            [test.utils.spies :as spies]
            [org.httpkit.server :as httpkit]
            [com.ben-allred.clj-app-simulator.services.emitter :as emitter]))

(deftest ^:unit sub-test
  (testing "(sub)"
    (let [on-close-spy (spies/create)
          send-spy (spies/create)
          stringify-spy (spies/create identity)
          yo-dawg-spy (spies/create (constantly stringify-spy))
          chan (reify httpkit/Channel
                 (send! [ch data]
                   (send-spy ch data))
                 (on-close [ch cb]
                   (on-close-spy ch cb)))
          socket-spy (spies/create (constantly chan))
          emitter (emitter/new)]
      (with-redefs [activity/socket-channel socket-spy
                    activity/accept->stringify yo-dawg-spy]
        (testing "when given a websocket request"
          (spies/reset! socket-spy on-close-spy)
          (let [request {:query-params {"accept" ::accept}
                         :headers      {"upgrade" "websocket"}}
                result (activity/sub request)
                chan-fn (last (first (spies/calls socket-spy)))
                _ (chan-fn emitter chan)
                close-fn (last (first (spies/calls on-close-spy)))]
            (is (spies/called-with? yo-dawg-spy ::accept))
            (testing "returns a socket channel"
              (is (spies/called-with? socket-spy request (spies/matcher fn?)))
              (is (= result chan)))
            (testing "and when an event is published"
              (spies/reset! send-spy stringify-spy send-spy)
              (emitter/publish emitter ::event ::data)
              (Thread/sleep 10)
              (testing "the event data is sent via websocket"
                (is (spies/called-with? stringify-spy {:event ::event :data ::data}))
                (is (spies/called-with? send-spy chan {:event ::event :data ::data}))))
            (testing "and when the websocket is closed"
              (is (spies/called-with? on-close-spy chan (spies/matcher fn?)))
              (close-fn nil)
              (testing "and when an event is published"
                (spies/reset! send-spy stringify-spy send-spy)
                (emitter/publish emitter ::event ::data)
                (testing "does not attempt to send data via websocket"
                  (is (spies/never-called? stringify-spy))
                  (is (spies/never-called? send-spy)))))))
        (testing "when not given a websocket request"
          (let [result (activity/sub {})]
            (testing "returns nil"
              (is (nil? result)))))))))
