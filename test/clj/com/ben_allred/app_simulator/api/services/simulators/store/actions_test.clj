(ns com.ben-allred.app-simulator.api.services.simulators.store.actions-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [com.ben-allred.app-simulator.api.services.simulators.store.actions :as actions]
    [com.ben-allred.app-simulator.services.content :as content]
    [immutant.web.async :as web.async]
    [test.utils.date-time :as test.dt]
    [test.utils.spies :as spies])
  (:import
    (java.util Date)))

(deftest ^:unit init-test
  (testing "(init)"
    (testing "wraps config in tuple"
      (is (= (actions/init {:some :value}) [:simulators/init {:some :value}])))))

(deftest ^:unit receive-test
  (testing "(receive)"
    (with-redefs [content/prepare (spies/create (comp first vector))]
      (let [actual (actions/receive {:extra        ::extra
                                     :stuff        ::stuff
                                     :socket-id    ::socket-id
                                     :headers      {"some" ::headers "accept" ::accept}
                                     :timestamp    ::timestamp
                                     :body         ::body
                                     :query-params {"some" ::query-params}
                                     :route-params ::route-params})
            expected {:headers      {:some ::headers :accept ::accept}
                      :body         ::body
                      :query-params {:some ::query-params}
                      :route-params ::route-params
                      :socket-id    ::socket-id}]
        (testing "cleans request"
          (is (spies/called-with? content/prepare (spies/matcher #(= (dissoc % :id) expected)) #{:content-type :accept} ::accept))
          (is (= expected (dissoc (second actual) :timestamp :id))))

        (testing "adds an id"
          (is (uuid? (:id (second actual)))))

        (testing "wraps cleaned request in a tuple"
          (is (= :simulators/receive (first actual))))

        (testing "adds timestamp"
          (let [timestamp (:timestamp (second (actions/receive {})))]
            (is (test.dt/date-within timestamp (Date.) 10))))))))

(deftest ^:unit change-test
  (testing "(change)"
    (testing "wraps config in a tuple"
      (is (= (actions/change {:some :value}) [:simulators/change {:some :value}])))))

(deftest ^:unit connect-test
  (testing "(connect)"
    (testing "wraps config in a tuple"
      (is (= (actions/connect ::socket-id ::ws)
             [:simulators.ws/connect ::socket-id ::ws])))))

(deftest ^:unit remove-socket-test
  (testing "(remove-socket)"
    (testing "wraps config in a tuple"
      (is (= (actions/remove-socket ::socket-id)
             [:simulators.ws/remove ::socket-id])))))

(deftest ^:unit send-one-test
  (testing "(send-one)"
    (with-redefs [web.async/send! (spies/create)]
      (let [state-spy (spies/constantly {:sockets {::id-1 ::ws-1 ::id-2 ::ws-2}})
            f (actions/send-one ::id-2 ::message)]
        (testing "sends the message to the socket"
          (spies/reset! web.async/send! state-spy)
          (f [nil state-spy])
          (is (spies/called-with? state-spy))
          (is (spies/called-with? web.async/send! ::ws-2 ::message))
          (is (spies/called-times? web.async/send! 1)))

        (testing "when no socket is found"
          (spies/reset! web.async/send! state-spy)
          (spies/respond-with! state-spy (constantly {:sockets {::id-1 ::ws}}))

          (testing "does not send the message"
            (is (spies/never-called? web.async/send!))))))))

(deftest ^:unit send-all-test
  (testing "(send-all)"
    (with-redefs [web.async/send! (spies/create)]
      (let [state-spy (spies/constantly {:sockets {1 ::ws-1 2 ::ws-2 3 ::ws-3}})
            f (actions/send-all ::message)]
        (testing "sends the message to every socket"
          (f [nil state-spy])

          (is (spies/called-with? web.async/send! ::ws-1 ::message))
          (is (spies/called-with? web.async/send! ::ws-2 ::message))
          (is (spies/called-with? web.async/send! ::ws-3 ::message)))))))

(deftest ^:unit disconnect-test
  (testing "(disconnect)"
    (with-redefs [web.async/close (spies/create)]
      (let [state-spy (spies/constantly {:sockets {::id-1 ::ws-1 ::id-2 ::ws-2}})
            f (actions/disconnect ::id-2)]
        (testing "disconnects the socket"
          (spies/reset! web.async/close state-spy)
          (f [nil state-spy])
          (is (spies/called-with? state-spy))
          (is (spies/called-with? web.async/close ::ws-2))
          (is (spies/called-times? web.async/close 1)))

        (testing "when no socket is found"
          (spies/reset! web.async/close state-spy)
          (spies/respond-with! state-spy (constantly {:sockets {::id-1 ::ws}}))

          (testing "does not disconnect"
            (is (spies/never-called? web.async/close))))))))

(deftest ^:unit disconnect-all-test
  (testing "(disconnect-all)"
    (with-redefs [web.async/close (spies/create)]
      (testing "sends the message to every socket"
        (actions/disconnect-all [nil (constantly {:sockets {1 ::ws-1 2 ::ws-2 3 ::ws-3}})])

        (is (spies/called-with? web.async/close ::ws-1))
        (is (spies/called-with? web.async/close ::ws-2))
        (is (spies/called-with? web.async/close ::ws-3))))))
