(ns com.ben-allred.app-simulator.services.navigation-test
  (:require
    [bidi.bidi :as bidi]
    [clojure.test :as t :refer [deftest is testing]]
    [com.ben-allred.app-simulator.services.navigation :as nav*]
    [com.ben-allred.app-simulator.utils.query-params :as qp]
    [test.utils.spies :as spies]))

(deftest ^:unit match-route*-test
  (testing "(match-route*)"
    (with-redefs [bidi/match-route (spies/constantly {:route :info})]
      (testing "calls bidi/match-route"
        (spies/reset! bidi/match-route)
        (nav*/match-route* ::routes "/some/path")
        (is (spies/called-with? bidi/match-route ::routes "/some/path")))

      (testing "returns route info"
        (with-redefs [qp/parse (spies/create)]
          (let [result (nav*/match-route* ::routes "/some/path")]
            (is (= {:route :info} result)))))

      (testing "includes query-params when they exist"
        (with-redefs [qp/parse (spies/constantly {:query :params})]
          (let [result (nav*/match-route* ::routes "/some/path?some=query&params=here")]
            (is (spies/called-with? qp/parse "some=query&params=here"))
            (is (= {:route :info :query-params {:query :params}} result))))))))

(deftest ^:unit path-for*-test
  (testing "(path-for*)"
    (with-redefs [bidi/path-for (spies/constantly "/some/route")]
      (testing "matches the route"
        (spies/reset! bidi/path-for)
        (nav*/path-for* ::routes ::page {:route-params :value})
        (is (spies/called-with? bidi/path-for ::routes ::page :route-params "value")))

      (testing "returns path string"
        (with-redefs [qp/stringify (spies/create)]
          (let [result (nav*/path-for* ::routes ::path {})]
            (is (= "/some/route" result)))))

      (testing "returns path with query params when they exist"
        (with-redefs [qp/stringify (spies/constantly "some=query&params=here")]
          (let [result (nav*/path-for* ::routes ::path {:query-params {:query :params}})]
            (is (spies/called-with? qp/stringify {:query :params}))
            (is (= "/some/route?some=query&params=here" result))))))))

(deftest ^:unit match-route-test
  (testing "(match-route)"
    (testing "calls match-route* with routes"
      (with-redefs [nav*/routes [::some ::routes]
                    nav*/match-route* (spies/constantly ::some-result)]
        (let [result (nav*/match-route ::path)]
          (is (= ::some-result result))
          (is (spies/called-with? nav*/match-route* [::some ::routes] ::path)))))))

(deftest ^:unit path-for-test
  (testing "(path-for)"
    (testing "calls path-for* with routes"
      (with-redefs [nav*/routes [::some ::routes]
                    nav*/path-for* (spies/constantly ::some-result)]
        (spies/reset! nav*/path-for*)
        (let [result (nav*/path-for ::path ::params)]
          (is (= ::some-result result))
          (is (spies/called-with? nav*/path-for* [::some ::routes] ::path ::params)))))

    (testing "defaults params to nil"
      (with-redefs [nav*/routes [::some ::routes]
                    nav*/path-for* (spies/constantly ::some-result)]
        (spies/reset! nav*/path-for*)
        (let [result (nav*/path-for ::path)]
          (is (= ::some-result result))
          (is (spies/called-with? nav*/path-for* [::some ::routes] ::path nil)))))))

(defn run-tests []
  (t/run-tests))
