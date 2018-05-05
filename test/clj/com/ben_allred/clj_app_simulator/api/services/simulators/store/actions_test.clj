(ns com.ben-allred.clj-app-simulator.api.services.simulators.store.actions-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.api.services.simulators.store.actions :as actions]
            [test.utils.date-time :as test.dt]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.services.content :as content])
  (:import [java.util Date]))

(deftest ^:unit init-test
  (testing "(init)"
    (testing "wraps config in tuple"
      (is (= (actions/init {:some :value}) [:simulators/init {:some :value}])))))

(deftest ^:unit receive-test
  (testing "(receive)"
    (let [prepare-spy (spies/create (comp first vector))]
      (with-redefs [content/prepare prepare-spy]
        (testing "cleans request"
          (let [actual (actions/receive {:extra        ::extra
                                         :stuff        ::stuff
                                         :headers      {"some" ::headers "accept" ::accept}
                                         :timestamp    ::timestamp
                                         :body         ::body
                                         :query-params {"some" ::query-params}
                                         :route-params ::route-params})
                expected {:headers      {:some ::headers :accept ::accept}
                          :body         ::body
                          :query-params {:some ::query-params}
                          :route-params ::route-params}]
            (is (spies/called-with? prepare-spy expected #{:content-type :accept} ::accept))
            (is (= expected (dissoc (second actual) :timestamp)))

            (testing "wraps cleaned request in a tuple"
              (is (= :simulators/receive (first actual))))

            (testing "adds timestamp"
              (let [timestamp (:timestamp (second (actions/receive {})))]
                (is (test.dt/date-within timestamp (Date.) 10))))))))))

(deftest ^:unit change-test
  (testing "(change)"
    (testing "wraps config in a tuple"
      (is (= (actions/change {:some :value}) [:http/change {:some :value}])))))
