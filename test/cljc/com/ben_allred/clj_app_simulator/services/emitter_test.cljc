(ns com.ben-allred.clj-app-simulator.services.emitter-test
  (:require
    #?@(:clj
        [[clojure.test :as t :refer [deftest is testing]]
         [test.utils.async :refer [async]]]
        :cljs
        [[cljs.test :as t :refer [async deftest is testing]]])
         [clojure.core.async :as async]
         [com.ben-allred.clj-app-simulator.services.emitter :as emitter]))

(defn ^:private init []
  [[(emitter/new)
    (emitter/new)]
   [(async/chan)
    (async/chan)
    (async/chan)]])

(deftest ^:unit new-test
  (testing "(new)"
    (testing "when subscribed"
      (async done
        (async/go
          (let [[[emitter-1 emitter-2] [all-chan event-1-chan event-2-chan]]
                (init)]
            (emitter/on emitter-1 ::env :event-1 event-1-chan)
            (emitter/on emitter-1 ::env :event-2 event-2-chan)
            (emitter/on emitter-1 ::env all-chan)
            (emitter/on emitter-2 ::env :event-1 event-2-chan)

            (testing "and when publishing"
              (emitter/publish emitter-1 ::env :event-1 {:some :data})
              (testing "places :event-1 on event-1-chan"
                (is (= [:event-1 {:some :data}] (async/<! event-1-chan))))

              (testing "places :event-1 on all-chan"
                (is (= [:event-1 {:some :data}] (async/<! all-chan))))

              (testing "nothing is placed on event-2-chan"
                (async/put! event-2-chan ::only-event)
                (is (= ::only-event (async/<! event-2-chan)))))

            (testing "when a channel has closed"
              (let [[[emitter] [chan-1 chan-2]]
                    (init)]
                (emitter/on emitter ::env :event chan-1)
                (emitter/on emitter ::env :event chan-2)
                (async/close! chan-1)

                (testing "continues to place events on open channels"
                  (emitter/publish emitter ::env :event {:some :data})
                  (is (= [:event {:some :data}] (async/<! chan-2))))))
            (done)))))))

(defn run-tests []
  (t/run-tests))
