(ns com.ben-allred.app-simulator.ui.services.store.activity-test
  (:require
    [clojure.test :as t :refer-macros [deftest is testing]]
    [com.ben-allred.app-simulator.services.env :as env]
    [com.ben-allred.app-simulator.services.ws :as ws]
    [com.ben-allred.app-simulator.ui.services.store.activity :as activity]
    [com.ben-allred.app-simulator.utils.transit :as transit]
    [test.utils.spies :as spies]))

(deftest ^:unit sub-test
  (testing "(sub)"
    (with-redefs [ws/connect (spies/create)
                  env/get (spies/constantly "some-host:123")]
      (let [dispatch-spy (spies/create)
            store {:dispatch dispatch-spy :store ::store}
            result (activity/sub store)
            [url & {:keys [on-err] :as opts}] (first (spies/calls ws/connect))]
        (testing "connects a websocket"
          (is (spies/called? ws/connect))
          (is (spies/called-with? env/get :host))
          (is (= url "ws://some-host:123/api/simulators/activity"))
          (is (= {:query-params {:accept "application/transit"}
                  :to-string    transit/stringify
                  :to-clj       transit/parse}
                 (dissoc opts :on-msg :on-err))))

        (testing "reconnects on error"
          (spies/reset! ws/connect)
          (on-err ::error)
          (let [[url] (first (spies/calls ws/connect))]
            (is (= url "ws://some-host:123/api/simulators/activity"))))

        (testing "returns the store"
          (is (= result store)))))))

(defn run-tests []
  (t/run-tests))
