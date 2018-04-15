(ns com.ben-allred.clj-app-simulator.ui.views.simulator-test
  (:require [cljs.test :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.ui.views.simulator :as sim]
            [test.utils.dom :as test.dom]))

(deftest ^:unit simulator-test
  (testing "(simulator)"
    (let [sim {:config   {::some ::simulator}
               :requests [{:timestamp 123
                           ::data     ::123}
                          {:timestamp 456
                           ::data     ::456}]}
          root (sim/sim sim)]
      (testing "displays a header"
        )
      (testing "displays sim-details"
        (let [details (test.dom/query-one root sim/sim-details)]
          (is (= [sim/sim-details sim] details))))
      (testing "displays sim-form"
        (let [form (test.dom/query-one root sim/sim-edit-form)]
          (is (= [sim/sim-edit-form sim] form))))
      (testing "displays a list of sim-request components"
        (let [[req-1 req-2 :as sim-reqs] (test.dom/query-all root sim/sim-request)]
          (is (= 2 (count sim-reqs)))
          (is (= "456" (:key (meta req-1))))
          (is (= [sim/sim-request {::some ::simulator} {:timestamp 456 ::data ::456}]
                 req-1))
          (is (= "123" (:key (meta req-2))))
          (is (= [sim/sim-request {::some ::simulator} {:timestamp 123 ::data ::123}]
                 req-2)))))))
