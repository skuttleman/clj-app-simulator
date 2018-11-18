(ns com.ben-allred.app-simulator.ui.services.store.activity-test
  (:require
    [clojure.test :as t :refer-macros [deftest is testing]]
    [com.ben-allred.app-simulator.services.env :as env]
    [com.ben-allred.app-simulator.services.ws :as ws]
    [com.ben-allred.app-simulator.ui.services.store.activity :as activity]
    [com.ben-allred.app-simulator.ui.utils.macros :as macros :include-macros true]
    [com.ben-allred.app-simulator.utils.transit :as transit]
    [test.utils.spies :as spies]))

(deftest ^:unit sub-test
  (testing "(sub)"
    (with-redefs [ws/connect (spies/create)
                  env/get (spies/constantly "some-host:123")
                  ws/close! (spies/create)
                  macros/set-timeout (spies/create)]
      (let [dispatch-spy (spies/create)
            store {:dispatch dispatch-spy :store ::store}
            result (activity/sub store)
            [url & {:keys [on-err on-close] :as opts}] (first (spies/calls ws/connect))]
        (testing "connects a websocket"
          (is (spies/called? ws/connect))
          (is (spies/called-with? env/get :host))
          (is (= url "ws://some-host:123/api/simulators/activity"))
          (is (= {:query-params {:accept "application/transit"}
                  :to-string    transit/stringify
                  :to-clj       transit/parse}
                 (dissoc opts :on-msg :on-err :on-close))))

        (testing "closes on error"
          (spies/reset! ws/connect)
          (on-err ::ws ::error)
          (is (spies/called-with? ws/close! ::ws)))

        (testing "reconnects on close"
          (on-close ::ws ::event)
          (is (spies/called-with? macros/set-timeout (spies/matcher fn?) 100))
          (let [[f] (first (spies/calls macros/set-timeout))]
            (spies/reset! ws/connect)
            (f)
            (is (spies/called? ws/connect))))

        (testing "returns the store"
          (is (= result store)))))))

(defn run-tests []
  (t/run-tests))
