(ns com.ben-allred.clj-app-simulator.api.services.simulators.store.actions-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.api.services.simulators.store.actions :as actions]
            [test.utils.date-time :as test.dt])
  (:import [java.util Date]))

(deftest ^:unit init-test
  (testing "(init)"
    (testing "wraps config in tuple"
      (is (= (actions/init {:some :value}) [:simulators/init {:some :value}])))))

(deftest ^:unit receive-test
  (testing "(receive)"
    (testing "cleans request"
      (let [actual (actions/receive {:extra        ::extra
                                     :stuff        ::stuff
                                     :headers      {"some" ::headers}
                                     :timesamp     ::timestamp
                                     :body         ::body
                                     :query-params {"some" ::query-params}
                                     :route-params ::route-params})
            expected {:headers      {:some ::headers}
                      :body         ::body
                      :query-params {:some ::query-params}
                      :route-params ::route-params}]
        (is (= :simulators/receive (first actual)))
        (is (= expected (dissoc (second actual) :timestamp)))))
    (testing "adds timestamp"
      (let [timestamp (:timestamp (second (actions/receive {})))]
        (is (test.dt/date-within timestamp (Date.) 10))))
    (testing "wraps cleaned request in a tuple")))

(deftest ^:unit change-test
  (testing "(change)"
    (testing "wraps config in a tuple"
      (is (= (actions/change {:some :value}) [:http/change {:some :value}])))))
