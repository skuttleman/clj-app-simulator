(ns com.ben-allred.clj-app-simulator.services.navigation-test
  (:require
    [bidi.bidi :as bidi]
    [clojure.test :as t :refer [deftest is testing]]
    [com.ben-allred.clj-app-simulator.services.navigation :as nav*]
    [com.ben-allred.clj-app-simulator.utils.query-params :as qp]
    [test.utils.spies :as spies]))

(deftest ^:unit match-route*-test
  (testing "(match-route*)"
    (let [match-route-spy (spies/constantly {:route :info})
          qp-spy (spies/constantly {:query :params})]
      (with-redefs [bidi/match-route match-route-spy]
        (testing "calls bidi/match-route"
          (spies/reset! match-route-spy)
          (nav*/match-route* ::routes "/some/path")
          (is (spies/called-with? match-route-spy ::routes "/some/path")))
        (testing "returns route info"
          (with-redefs [qp/parse (spies/create)]
            (let [result (nav*/match-route* ::routes "/some/path")]
              (is (= {:route :info} result)))))
        (testing "includes query-params when they exist"
          (with-redefs [qp/parse qp-spy]
            (let [result (nav*/match-route* ::routes "/some/path?some=query&params=here")]
              (is (spies/called-with? qp-spy "some=query&params=here"))
              (is (= {:route :info :query-params {:query :params}} result)))))))))

(deftest ^:unit path-for*-test
  (testing "(path-for*)"
    (let [path-for-spy (spies/constantly "/some/route")
          qp-spy (spies/constantly "some=query&params=here")]
      (with-redefs [bidi/path-for path-for-spy]
        (testing "calls bidi/path-for"
          (spies/reset! path-for-spy)
          (nav*/path-for* ::routes ::page {:route-params :value})
          (is (spies/called-with? path-for-spy ::routes ::page :route-params "value")))
        (testing "returns path string"
          (with-redefs [qp/stringify (spies/create)]
            (let [result (nav*/path-for* ::routes ::path {})]
              (is (= "/some/route" result)))))
        (testing "returns path with query params when they exist"
          (with-redefs [qp/stringify qp-spy]
            (let [result (nav*/path-for* ::routes ::path {:query-params {:query :params}})]
              (is (spies/called-with? qp-spy {:query :params}))
              (is (= "/some/route?some=query&params=here" result)))))))))

(deftest ^:unit match-route-test
  (testing "(match-route)"
    (testing "calls match-route* with routes"
      (let [match-route-spy (spies/constantly ::some-result)]
        (with-redefs [nav*/routes [::some ::routes]
                      nav*/match-route* match-route-spy]
          (let [result (nav*/match-route ::path)]
            (is (= ::some-result result))
            (is (spies/called-with? match-route-spy [::some ::routes] ::path))))))))

(deftest ^:unit path-for-test
  (testing "(path-for)"
    (let [path-for-spy (spies/constantly ::some-result)]
      (testing "calls path-for* with routes"
        (with-redefs [nav*/routes [::some ::routes]
                      nav*/path-for* path-for-spy]
          (spies/reset! path-for-spy)
          (let [result (nav*/path-for ::path ::params)]
            (is (= ::some-result result))
            (is (spies/called-with? path-for-spy [::some ::routes] ::path ::params)))))
      (testing "defaults params to nil"
        (with-redefs [nav*/routes [::some ::routes]
                      nav*/path-for* path-for-spy]
          (spies/reset! path-for-spy)
          (let [result (nav*/path-for ::path)]
            (is (= ::some-result result))
            (is (spies/called-with? path-for-spy [::some ::routes] ::path nil))))))))

(defn run-tests []
  (t/run-tests))
