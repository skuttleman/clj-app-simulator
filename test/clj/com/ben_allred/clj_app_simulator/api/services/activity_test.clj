(ns com.ben-allred.clj-app-simulator.api.services.activity-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.api.services.activity :as activity]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.services.emitter :as emitter]
            [immutant.web.async :as web.async]))

(deftest ^:unit sub-test
  (testing "(sub)"
    (let [send-spy (spies/create)
          stringify-spy (spies/create identity)
          yo-dawg-spy (spies/constantly stringify-spy)
          socket-spy (spies/constantly ::websocket)
          emitter (emitter/new)]
      (with-redefs [web.async/as-channel socket-spy
                    web.async/send! send-spy
                    activity/accept->stringify yo-dawg-spy
                    activity/emitter emitter]
        (testing "when given a websocket request"
          (spies/reset! socket-spy)
          (let [request {:query-params {"accept" ::accept}
                         :websocket?   true}
                result (activity/sub request)
                {:keys [on-open on-close]} (last (first (spies/calls socket-spy)))]
            (is (spies/called-with? yo-dawg-spy ::accept))
            (on-open ::websocket)
            (testing "returns a socket channel"
              (is (spies/called-with? socket-spy request (spies/matcher map?)))
              (is (= result ::websocket)))

            (testing "and when an event is published"
              (spies/reset! send-spy stringify-spy send-spy)
              (emitter/publish emitter ::event ::data)
              (Thread/sleep 25)
              (testing "sends the event data via websocket"
                (is (spies/called-with? stringify-spy {:event ::event :data ::data}))
                (is (spies/called-with? send-spy ::websocket {:event ::event :data ::data}))))

            (testing "and when the websocket is closed"
              (on-close nil nil)
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
