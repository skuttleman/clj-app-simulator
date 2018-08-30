(ns com.ben-allred.clj-app-simulator.ui.services.store.activity-test
  (:require [clojure.test :as t :refer-macros [deftest testing is]]
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
          env-spy (spies/constantly "some-host:123")]
      (with-redefs [ws/connect ws-spy
                    env/get env-spy]
        (let [result (activity/sub store)
              [url & {:keys [on-err on-msg] :as opts}] (first (spies/calls ws-spy))]
          (testing "connects a websocket"
            (is (spies/called? ws-spy))
            (is (spies/called-with? env-spy :host))
            (is (= url "ws://some-host:123/api/simulators/activity"))
            (is (= {:query-params {:accept "application/transit"}
                    :to-string    transit/stringify
                    :to-clj       transit/parse}
                   (dissoc opts :on-msg :on-err))))

          (testing "reconnects on error"
            (spies/reset! ws-spy)
            (on-err ::error)
            (let [[url] (first (spies/calls ws-spy))]
              (is (= url "ws://some-host:123/api/simulators/activity"))))

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

          (testing "dispatches on :simulators/reset-requests"
            (spies/reset! dispatch-spy)
            (on-msg {:event :simulators/reset-requests :data ::simulator})
            (is (spies/called-with? dispatch-spy [:simulators.activity/reset-requests {:simulator ::simulator}])))

          (testing "dispatches on :simulators/change"
            (spies/reset! dispatch-spy)
            (on-msg {:event :simulators/change :data ::simulator})
            (is (spies/called-with? dispatch-spy [:simulators.activity/change {:simulator ::simulator}])))

          (testing "dispatches on :ws/connect"
            (spies/reset! dispatch-spy)
            (on-msg {:event :ws/connect :data ::data})
            (is (spies/called-with? dispatch-spy [:simulators.activity/connect {:simulator ::data}])))

          (testing "dispatches on :ws/disconnect"
            (spies/reset! dispatch-spy)
            (on-msg {:event :ws/disconnect :data ::data})
            (is (spies/called-with? dispatch-spy [:simulators.activity/disconnect {:simulator ::data}])))

          (testing "returns the store"
            (is (= result store))))))))

(defn run-tests [] (t/run-tests))
