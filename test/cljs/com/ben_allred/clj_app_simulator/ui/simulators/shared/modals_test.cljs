(ns com.ben-allred.clj-app-simulator.ui.simulators.shared.modals-test
  (:require [cljs.test :as t :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.modals :as modals]
            [test.utils.dom :as test.dom]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.ui.utils.moment :as mo]
            [com.ben-allred.clj-app-simulator.utils.strings :as strings]))

(deftest ^:unit sim-iterate-test
  (testing "(sim-iterate)"
    (let [xform-spy (spies/create identity)
          root (modals/sim-iterate "a label"
                                   {:two   ["a" "list" "of" "things"]
                                    :one   "thing"
                                    :three "another thing"}
                                   ::class
                                   xform-spy)]
      (testing "uses class on root node"
        (is (= ::class (:class-name (test.dom/attrs root)))))
      (testing "displays the label"
        (is (test.dom/contains? root "a label")))
      (testing "when displaying key-vals"
        (let [key-vals (test.dom/query-one root :.key-vals)
              [li-1 li-2 li-3] (test.dom/query-all key-vals :li)]
          (testing "sorts by key"
            (is (= ":one" (:key (test.dom/attrs li-1))))
            (is (= ":two" (:key (test.dom/attrs li-3))))
            (is (= ":three" (:key (test.dom/attrs li-2)))))
          (testing "displays the transformed key"
            (is (spies/called-with? xform-spy "one"))
            (is (test.dom/contains? (test.dom/query-one li-1 :.key) "one"))
            (is (spies/called-with? xform-spy "two"))
            (is (test.dom/contains? (test.dom/query-one li-3 :.key) "two"))
            (is (spies/called-with? xform-spy "three"))
            (is (test.dom/contains? (test.dom/query-one li-2 :.key) "three")))
          (testing "displays the val"
            (is (-> li-1
                    (test.dom/query-one :.val)
                    (test.dom/contains? "thing")))
            (is (-> li-2
                    (test.dom/query-one :.val)
                    (test.dom/contains? "another thing"))))
          (testing "and when the val is a collection"
            (testing "joins the coll"
              (is (-> li-3
                      (test.dom/query-one :.val)
                      (test.dom/contains? "a,list,of,things"))))))))))

(deftest ^:unit request-modal-test
  (testing "(request-modal)"
    (let [format-spy (spies/constantly ::formatted)]
      (with-redefs [mo/format format-spy]
        (let [qp {:a 1 :b 2}
              headers {:header-1 "val-1" :header-2 "val-2"}
              root (modals/request-modal {:method ::method :path "/some/path"}
                                         {:dt           ::moment
                                          :query-params qp
                                          :headers      headers
                                          :body         "this is a body"})
              details (test.dom/query-one root :.request-details)]
          (testing "displays the method and path"
            (is (test.dom/contains? details [:* "METHOD" ": " "/some/path"])))

          (testing "formats the dt moment"
            (is (spies/called-with? format-spy ::moment)))

          (testing "displays the formatted moment"
            (is (test.dom/contains? details ::formatted)))

          (testing "when there are query params"
            (testing "iterates over query params"
              (is (test.dom/contains? details
                                      [modals/sim-iterate "Query:" qp "query-params"]))))

          (testing "when there are headers"
            (testing "iterates over headers"
              (is (test.dom/contains? details
                                      [modals/sim-iterate "Headers:" headers "headers" strings/titlize]))))

          (testing "when there is a body"
            (let [request-body (test.dom/query-one details :.request-body)]
              (testing "displays a label for the body"
                (is (test.dom/contains? request-body "Body:")))

              (testing "displays the body"
                (is (test.dom/contains? request-body "this is a body"))))))

        (testing "when there are no query params"
          (let [root (modals/request-modal {:method ::method} {})
                iterate (test.dom/query-all root modals/sim-iterate)]
            (testing "does not iterate query-params"
              (is (not (test.dom/contains? iterate "Query:")))
              (is (not (test.dom/contains? iterate "query-params"))))))

        (testing "when there are no headers"
          (let [root (modals/request-modal {:method ::method} {})
                iterate (test.dom/query-all root modals/sim-iterate)]
            (testing "does not iterate headers"
              (is (not (test.dom/contains? iterate "Headers:")))
              (is (not (test.dom/contains? iterate "headers"))))))

        (testing "when there is no body"
          (let [details (modals/request-modal {:method ::method} {})]
            (testing "does not display a body"
              (is (not (test.dom/contains? details :.request-body))))))))))

(defn run-tests []
  (t/run-tests))
