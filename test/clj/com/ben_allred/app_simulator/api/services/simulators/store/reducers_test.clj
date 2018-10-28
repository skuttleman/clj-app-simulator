(ns com.ben-allred.app-simulator.api.services.simulators.store.reducers-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [com.ben-allred.app-simulator.api.services.simulators.store.reducers :as reducers]))

(deftest ^:unit simulator-config-test
  (testing "(simulator-config)"
    (testing "has default state"
      (is (nil? (reducers/simulator-config))))

    (testing "responds to :simulators/init"
      (is (= {:initial ::config :current ::config}
             (reducers/simulator-config ::anything [:simulators/init ::config]))))

    (testing "responds to :simulators/reset"
      (is (= {:initial ::initial :current ::initial}
             (reducers/simulator-config {:initial ::initial :current ::current} [:simulators/reset]))))

    (testing "responds to :simulators/reset-config"
      (is (= {:initial ::initial :current ::initial}
             (reducers/simulator-config {:initial ::initial :current ::current} [:simulators/reset-config]))))

    (testing "responds to :simulators/change"
      (is (= {:current {:a 1 :b 2 :response {:c 3 :d 4}}}
             (reducers/simulator-config {:current {:a 1 :response {:c 3}}}
                                        [:simulators/change {:b 2 :response {:d 4}}]))))

    (testing "returns state for all other types"
      (is (= ::state (reducers/simulator-config ::state [(keyword (rand-int 1000))]))))))

(deftest ^:unit http-requests-test
  (testing "(http-requests)"
    (testing "has default state"
      (is (= [] (reducers/http-requests))))

    (testing "responds to :simulators/init"
      (is (= [] (reducers/http-requests ::anything [:simulators/init]))))

    (testing "responds to :simulators/reset"
      (is (= [] (reducers/http-requests ::anything [:simulators/reset]))))

    (testing "responds to :simulators.http/reset-requests"
      (is (= [] (reducers/http-requests ::anything [:simulators.http/reset-requests]))))

    (testing "responds to :simulators/receive"
      (let [actual (reducers/http-requests [::previous] [:simulators/receive ::current])]
        (is (= [::previous ::current] actual))))

    (testing "returns state for all other types"
      (is (= ::state (reducers/http-requests ::state [(keyword (rand-int 1000))]))))))

(deftest ^:unit ws-requests-test
  (testing "(ws-requests)"
    (testing "has default state"
      (is (= [] (reducers/ws-requests))))

    (testing "responds to :simulators/init"
      (is (= [] (reducers/ws-requests ::anything [:simulators/init]))))

    (testing "responds to :simulators/reset"
      (is (= [] (reducers/ws-requests ::anything [:simulators/reset]))))

    (testing "responds to :simulators.ws/reset-messages"
      (is (= [] (reducers/ws-requests ::anything [:simulators.ws/reset-messages]))))

    (testing "responds to :simulators/receive"
      (let [actual (reducers/ws-requests [::previous] [:simulators/receive ::current])]
        (is (= [::previous ::current] actual))))

    (testing "returns state for all other types"
      (is (= ::state (reducers/ws-requests ::state [(keyword (rand-int 1000))]))))))

(deftest ^:unit http-config-test
  (testing "(http-config)"
    (testing "has default state"
      (is (nil? (reducers/http-config))))

    (testing "responds to :simulators/init"
      (let [expected {:initial ::config :current ::config}
            actual (reducers/http-config ::any-state [:simulators/init ::config])]
        (is (= expected actual))))

    (testing "responds to :simulators/reset"
      (let [expected {:initial ::initial :current ::initial}
            actual (reducers/http-config {:initial ::initial} [:simulators/reset])]
        (is (= expected actual))))

    (testing "responds to :simulators/change"
      (let [expected {:current {:a 1 :b 2 :response {:c 3 :d 4}}}
            actual (reducers/http-config {:current {:a 1 :response {:d 4}}}
                                         [:simulators/change {:b 2 :response {:c 3}}])]
        (is (= expected actual))))

    (testing "responds to :simulators.http/reset-response"
      (let [expected {:initial ::initial :current ::initial}
            actual (reducers/http-config {:initial ::initial} [:simulators.http/reset-response])]
        (is (= expected actual))))

    (testing "returns state for all other types"
      (is (= ::state (reducers/http-config ::state [(keyword (rand-int 1000))]))))))

(deftest ^:unit ws-sockets-test
  (testing "(ws-sockets)"
    (testing "responds to :simulators.ws/connect"
      (is (= {::id-1 ::ws-1 ::id-2 ::ws-2}
             (reducers/ws-sockets {::id-1 ::ws-1} [:simulators.ws/connect ::id-2 ::ws-2]))))

    (testing "responds to :simulators.ws/remove"
      (is (= {::id-1 ::ws-1 ::id-2 nil}
             (reducers/ws-sockets {::id-1 ::ws-1 ::id-2 ::ws-2} [:simulators.ws/remove ::id-2]))))

    (testing "response to :simulators/reset"
      (is (empty? (reducers/ws-sockets {::id-1 ::ws-1 ::id-2 ::ws-2} [:simulators/reset]))))

    (testing "returns state for all other types"
      (is (= {::some ::state} (reducers/ws-sockets {::some ::state} [::anything ::id ::ws]))))))
