(ns com.ben-allred.app-simulator.api.services.activity-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [com.ben-allred.app-simulator.api.services.activity :as activity]
    [com.ben-allred.app-simulator.services.emitter :as emitter]
    [immutant.web.async :as web.async]
    [test.utils.spies :as spies]))

(deftest ^:unit sub-test
  (testing "(sub)"
    (let [stringify-spy (spies/create identity)
          accept-spy (spies/constantly stringify-spy)
          emitter (emitter/new)]
      (with-redefs [web.async/as-channel (spies/constantly ::websocket)
                    web.async/send! (spies/create)
                    activity/accept->stringify accept-spy
                    activity/emitter emitter]
        (testing "when given a websocket request"
          (spies/reset! web.async/as-channel)
          (let [request {:query-params {"accept" ::accept}
                         :websocket?   true}
                result (activity/sub ::env request)
                {:keys [on-open on-close]} (last (first (spies/calls web.async/as-channel)))]
            (is (spies/called-with? accept-spy ::accept))
            (on-open ::websocket)
            (testing "returns a socket channel"
              (is (spies/called-with? web.async/as-channel request (spies/matcher map?)))
              (is (= result ::websocket)))

            (testing "and when an event is published"
              (spies/reset! stringify-spy web.async/send!)
              (emitter/publish emitter ::env ::event ::data)
              (Thread/sleep 50)
              (testing "sends the event data via websocket"
                (is (spies/called-with? stringify-spy {:event ::event :data ::data}))
                (is (spies/called-with? web.async/send! ::websocket {:event ::event :data ::data}))))

            (testing "and when the websocket is closed"
              (on-close nil nil)
              (testing "and when an event is published"
                (spies/reset! stringify-spy web.async/send!)
                (emitter/publish emitter ::env ::event ::data)
                (testing "does not attempt to send data via websocket"
                  (is (spies/never-called? stringify-spy))
                  (is (spies/never-called? web.async/send!)))))))

        (testing "when not given a websocket request"
          (let [result (activity/sub ::env {})]
            (testing "returns nil"
              (is (nil? result)))))))))
