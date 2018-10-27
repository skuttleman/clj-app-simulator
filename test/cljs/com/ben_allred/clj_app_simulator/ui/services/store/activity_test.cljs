(ns com.ben-allred.clj-app-simulator.ui.services.store.activity-test
  (:require
    [clojure.test :as t :refer-macros [deftest is testing]]
    [com.ben-allred.clj-app-simulator.services.env :as env]
    [com.ben-allred.clj-app-simulator.services.ws :as ws]
    [com.ben-allred.clj-app-simulator.ui.services.store.activity :as activity]
    [com.ben-allred.clj-app-simulator.utils.transit :as transit]
    [test.utils.spies :as spies]))

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

          (testing "returns the store"
            (is (= result store))))))))

(defn run-tests []
  (t/run-tests))
