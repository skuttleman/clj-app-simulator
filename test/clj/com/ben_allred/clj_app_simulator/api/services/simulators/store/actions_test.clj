(ns com.ben-allred.clj-app-simulator.api.services.simulators.store.actions-test
    (:require [clojure.test :refer [deftest testing is]]
              [com.ben-allred.clj-app-simulator.api.services.simulators.store.actions :as actions]))

(deftest init-test
    (testing "(init)"
        (testing "wraps config in tuple"
            (is (= (actions/init {:some :value}) [:simulators/init {:some :value}])))))
