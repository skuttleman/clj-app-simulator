(ns com.ben-allred.clj-app-simulator.utils.colls-test
  (:require [clojure.test :refer [deftest testing is are]]
            [com.ben-allred.clj-app-simulator.utils.colls :as colls]))

(deftest ^:unit force-sequential-test
  (testing "(force-sequential)"
    (are [input expected] (= (colls/force-sequential input) expected)
      nil nil
      1 [1]
      :keyword [:keyword]
      [1 2 3] [1 2 3]
      () ()
      #{1 2 3} [#{1 2 3}]
      {:a 1 :b 2} [{:a 1 :b 2}])))
