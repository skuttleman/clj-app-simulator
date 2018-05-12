(ns com.ben-allred.clj-app-simulator.ui.services.store.activity-test
  (:require [cljs.test :as t :refer-macros [deftest testing is]]
            [com.ben-allred.clj-app-simulator.ui.services.store.activity :as activity]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.services.ws :as ws]
            [com.ben-allred.clj-app-simulator.services.env :as env]
            [com.ben-allred.clj-app-simulator.utils.transit :as transit]))

(deftest ^:unit sub-test
  (testing "(sub)"
    (let [dispatch-spy (spies/create)
          store {:dispatch dispatch-spy :store ::store}
          ws-spy (spies/create)
          env-spy (spies/create (constantly "some-host:123"))]
      (with-redefs [ws/connect ws-spy
                    env/get env-spy]
        (let [result (activity/sub store)
              [url & {on-msg :on-msg :as opts}] (first (spies/calls ws-spy))]
          (testing "connects a websocket"
            (is (spies/called? ws-spy))
            (is (spies/called-with? env-spy :host))
            (is (= url "ws://some-host:123/api/simulators/activity"))
            (is (= {:query-params {:accept "application/transit"}
                    :to-string    transit/stringify
                    :to-clj       transit/parse}
                   (dissoc opts :on-msg))))

          (testing "dispatches on :simulators/init"
            (spies/reset! dispatch-spy)
            (on-msg {:event :simulators/init :data ::data})
            (is (spies/called-with? dispatch-spy [:simulators.fetch-all/succeed {:simulators ::data}])))

          (testing "dispatches on :simulators/receive"
            (spies/reset! dispatch-spy)
            (on-msg {:event :simulators/receive :data ::data})
            (is (spies/called-with? dispatch-spy [:simulators.activity/receive ::data])))

          (testing "dispatches on :simulators/add"
            (spies/reset! dispatch-spy)
            (on-msg {:event :simulators/add :data ::data})
            (is (spies/called-with? dispatch-spy [:simulators.activity/add {:simulator ::data}])))

          (testing "dispatches on :simulators/delete"
            (spies/reset! dispatch-spy)
            (on-msg {:event :simulators/delete :data ::data})
            (is (spies/called-with? dispatch-spy [:simulators.activity/delete ::data])))

          (testing "dispatches on :http/reset-requests"
            (spies/reset! dispatch-spy)
            (on-msg {:event :http/reset-requests :data ::simulator})
            (is (spies/called-with? dispatch-spy [:simulators.activity/reset-requests {:simulator ::simulator}])))

          (testing "dispatches on :http/change"
            (spies/reset! dispatch-spy)
            (on-msg {:event :http/change :data ::simulator})
            (is (spies/called-with? dispatch-spy [:simulators.activity/change {:simulator ::simulator}])))

          (testing "dispatches on :ws/connect"
            (spies/reset! dispatch-spy)
            (on-msg {:event :ws/connect :data {:id ::id :socket-id ::socket-id :simulator ::simulator}})
            (is (spies/called-with? dispatch-spy [:simulators.activity/connect {:id ::id :socket-id ::socket-id}])))

          (testing "dispatches on :ws/disconnect"
            (spies/reset! dispatch-spy)
            (on-msg {:event :ws/disconnect :data {:id ::id :socket-id ::socket-id :simulator ::simulator}})
            (is (spies/called-with? dispatch-spy [:simulators.activity/disconnect {:id ::id :socket-id ::socket-id}])))

          (testing "returns the store"
            (is (= result store))))))))

(defn run-tests [] (t/run-tests))
