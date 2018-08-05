(ns com.ben-allred.clj-app-simulator.services.ws-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.services.ws :as ws]
            [test.utils.spies :as spies]
            [gniazdo.core :as gniazdo]))

(deftest ^:unit connect-test
  (testing "(connect)"
    (let [gniazdo-spy (spies/create)
          open-spy (spies/create)
          close-spy (spies/create)
          msg-spy (spies/create)
          err-spy (spies/create)
          clj-spy (spies/create str)]
      (with-redefs [gniazdo/connect gniazdo-spy]
        (let [ws (ws/connect "some-url"
                             :query-params {:some :qp}
                             :on-open open-spy
                             :on-close close-spy
                             :on-msg msg-spy
                             :on-err err-spy
                             :to-string ::to-string
                             :to-clj clj-spy)
              _ (ws/connect "some-other-url")
              [url & {:as opts}] (first (spies/calls gniazdo-spy))
              [url-2 & {:as opts-2}] (second (spies/calls gniazdo-spy))]
          (testing "creates a websocket with query-params"
            (is (= "some-url?some=qp" url)))
          (testing "creates a websocket without query-params"
            (is (= "some-other-url" url-2)))
          (testing "when connecting a websocket"
            ((:on-connect opts) ::event)
            (testing "calls :on-open"
              (is (spies/called-with? open-spy ::event))))
          (testing "when a message is received"
            ((:on-receive opts) :a-message)
            (testing "parses the message"
              (spies/called-with? clj-spy :a-message))
            (testing "calls :on-msg"
              (spies/called-with? msg-spy ":a-message")))
          (testing "when an error happens"
            ((:on-error opts) ::error)
            (testing "calls :on-err"
              (is (spies/called-with? err-spy ::error))))
          (testing "when the socket is closed"
            ((:on-close opts) ::code ::reason)
            (testing "calls :on-close"
              (is (spies/called-with? close-spy [::code ::reason]))))
          (testing "adds to-string meta data"
            (let [to-string (:com.ben-allred.clj-app-simulator.services.ws/to-string (meta ws))]
              (is (= to-string ::to-string))))
          (testing "does not blow up for missing event-handlers"
            ((:on-connect opts-2) ::open)
            ((:on-receive opts-2) ::message)
            ((:on-error opts-2) ::error)
            ((:on-close opts-2) ::code ::reason)))))))

(deftest ^:unit send!-test
  (testing "(send!)"
    (let [gniazdo-spy (spies/create)
          to-str-spy (spies/create str)
          ws (with-meta [::ws] {:com.ben-allred.clj-app-simulator.services.ws/to-string to-str-spy})]
      (with-redefs [gniazdo/send-msg gniazdo-spy]
        (testing "when sending a message"
          (ws/send! ws :a-message)
          (testing "calls to-string on the message"
            (is (spies/called-with? to-str-spy :a-message)))

          (testing "sends the message via socket"
            (is (spies/called-with? gniazdo-spy ::ws ":a-message"))))))))
