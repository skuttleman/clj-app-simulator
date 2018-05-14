(ns com.ben-allred.clj-app-simulator.api.services.simulators.store.reducers-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.api.services.simulators.store.reducers :as reducers]))

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

    (testing "responds to :simulators/change"
      (is (= {:current {:a 1 :b {:c 2 :d 3} :e 4}}
             (reducers/simulator-config {:current {:a 1 :b {:c 2}}}
                                        [:simulators/change {:e 4 :b {:d 3}}]))))

    (testing "returns state for all other types"
      (is (= ::state (reducers/simulator-config ::state [(keyword (rand-int 1000))]))))))

(deftest ^:unit simulator-requests-test
  (testing "(simulator-requests)"
    (testing "has default state"
      (is (= [] (reducers/simulator-requests))))

    (testing "responds to :simulators/init"
      (is (= [] (reducers/simulator-requests ::anything [:simulators/init]))))

    (testing "responds to :simulators/reset"
      (is (= [] (reducers/simulator-requests ::anything [:simulators/reset]))))

    (testing "responds to :simulators/reset-requests"
      (is (= [] (reducers/simulator-requests ::anything [:simulators/reset-requests]))))

    (testing "responds to :simulators/receive"
      (let [actual (reducers/simulator-requests [::previous] [:simulators/receive ::current])]
        (is (= [::previous ::current] actual))))

    (testing "returns state for all other types"
      (is (= ::state (reducers/simulator-requests ::state [(keyword (rand-int 1000))]))))))

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
      (let [expected {:current {:a {:b [4 5 6] :c {:d :e :f :g}}}}
            actual (reducers/http-config {:current {:a {:b [1 2 3] :c {:d :e}}}}
                                         [:simulators/change {:a {:b [4 5 6] :c {:f :g}}}])]
        (is (= expected actual))))

    (testing "responds to :http/reset-response"
      (let [expected {:initial ::initial :current ::initial}
            actual (reducers/http-config {:initial ::initial} [:http/reset-response])]
        (is (= expected actual))))

    (testing "returns state for all other types"
      (is (= ::state (reducers/http-config ::state [(keyword (rand-int 1000))]))))))

(deftest ^:unit ws-sockets-test
  (testing "(ws-sockets)"
    (testing "responds to :ws/connect"
      (is (= {::id-1 ::ws-1 ::id-2 ::ws-2}
             (reducers/ws-sockets {::id-1 ::ws-1} [:ws/connect ::id-2 ::ws-2]))))

    (testing "responds to :ws/remove"
      (is (= {::id-1 ::ws-1 ::id-2 nil}
             (reducers/ws-sockets {::id-1 ::ws-1 ::id-2 ::ws-2} [:ws/remove ::id-2]))))

    (testing "response to :simulators/reset"
      (is (empty? (reducers/ws-sockets {::id-1 ::ws-1 ::id-2 ::ws-2} [:simulators/reset]))))

    (testing "returns state for all other types"
      (is (= {::some ::state} (reducers/ws-sockets {::some ::state} [::anything ::id ::ws]))))))
