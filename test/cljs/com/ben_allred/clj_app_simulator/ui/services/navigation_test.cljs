(ns com.ben-allred.clj-app-simulator.ui.services.navigation-test
  (:require [cljs.test :as t  :refer-macros [deftest testing is]]
            [bidi.bidi :as bidi]
            [com.ben-allred.clj-app-simulator.utils.query-params :as qp]
            [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
            [test.utils.spies :as spies]
            [pushy.core :as pushy]))

(deftest ^:unit match-route*-test
  (testing "(match-route*)"
    (let [match-route-spy (spies/constantly {:route :info})
          qp-spy (spies/constantly {:query :params})]
      (with-redefs [bidi/match-route match-route-spy]
        (testing "calls bidi/match-route"
          (spies/reset! match-route-spy)
          (nav/match-route* ::routes "/some/path")
          (is (spies/called-with? match-route-spy ::routes "/some/path")))
        (testing "returns route info"
          (with-redefs [qp/parse (spies/create)]
            (let [result (nav/match-route* ::routes "/some/path")]
              (is (= {:route :info} result)))))
        (testing "includes query-params when they exist"
          (with-redefs [qp/parse qp-spy]
            (let [result (nav/match-route* ::routes "/some/path?some=query&params=here")]
              (is (spies/called-with? qp-spy "some=query&params=here"))
              (is (= {:route :info :query-params {:query :params}} result)))))))))

(deftest ^:unit path-for*-test
  (testing "(path-for*)"
    (let [path-for-spy (spies/constantly "/some/route")
          qp-spy (spies/constantly "some=query&params=here")]
      (with-redefs [bidi/path-for path-for-spy]
        (testing "calls bidi/path-for"
          (spies/reset! path-for-spy)
          (nav/path-for* ::routes ::page {:route-params :value})
          (is (spies/called-with? path-for-spy ::routes ::page :route-params "value")))
        (testing "returns path string"
          (with-redefs [qp/stringify (spies/create)]
            (let [result (nav/path-for* ::routes ::path {})]
              (is (= "/some/route" result)))))
        (testing "returns path with query params when they exist"
          (with-redefs [qp/stringify qp-spy]
            (let [result (nav/path-for* ::routes ::path {:query-params {:query :params}})]
              (is (spies/called-with? qp-spy {:query :params}))
              (is (= "/some/route?some=query&params=here" result)))))))))

(deftest ^:unit match-route-test
  (testing "(match-route)"
    (testing "calls match-route* with routes"
      (let [match-route-spy (spies/constantly ::some-result)]
        (with-redefs [nav/routes [::some ::routes]
                      nav/match-route* match-route-spy]
          (let [result (nav/match-route ::path)]
            (is (= ::some-result result))
            (is (spies/called-with? match-route-spy [::some ::routes] ::path))))))))

(deftest ^:unit path-for-test
  (testing "(path-for)"
    (let [path-for-spy (spies/constantly ::some-result)]
      (testing "calls path-for* with routes"
        (with-redefs [nav/routes [::some ::routes]
                      nav/path-for* path-for-spy]
          (spies/reset! path-for-spy)
          (let [result (nav/path-for ::path ::params)]
            (is (= ::some-result result))
            (is (spies/called-with? path-for-spy [::some ::routes] ::path ::params)))))
      (testing "defaults params to nil"
        (with-redefs [nav/routes [::some ::routes]
                      nav/path-for* path-for-spy]
          (spies/reset! path-for-spy)
          (let [result (nav/path-for ::path)]
            (is (= ::some-result result))
            (is (spies/called-with? path-for-spy [::some ::routes] ::path nil))))))))

(deftest ^:unit navigate*-test
  (testing "(navigate*)"
    (let [pushy-spy (spies/create)
          path-for-spy (spies/constantly ::path)]
      (testing "sets history token"
        (with-redefs [pushy/set-token! pushy-spy
                      nav/path-for* path-for-spy]
          (nav/navigate* ::history ::routes ::page ::params)
          (is (spies/called-with? path-for-spy ::routes ::page ::params))
          (is (spies/called-with? pushy-spy ::history ::path)))))))

(deftest ^:unit nav-and-replace*-test
  (testing "(nav-and-replace*)"
    (let [pushy-spy (spies/create)
          path-for-spy (spies/constantly ::path)]
      (testing "sets history token"
        (with-redefs [pushy/replace-token! pushy-spy
                      nav/path-for* path-for-spy]
          (nav/nav-and-replace* ::history ::routes ::page ::params)
          (is (spies/called-with? path-for-spy ::routes ::page ::params))
          (is (spies/called-with? pushy-spy ::history ::path)))))))

(deftest ^:unit navigate!-test
  (testing "(navigate!)"
    (let [navigate-spy (spies/create)]
      (testing "calls navigate* with routes"
        (with-redefs [nav/history ::history
                      nav/routes [::some ::routes]
                      nav/navigate* navigate-spy]
          (spies/reset! navigate-spy)
          (nav/navigate! ::page ::params)
          (is (spies/called-with? navigate-spy ::history [::some ::routes] ::page ::params))))
      (testing "defaults params to nil"
        (with-redefs [nav/history ::history
                      nav/routes [::some ::routes]
                      nav/navigate* navigate-spy]
          (spies/reset! navigate-spy)
          (nav/navigate! ::page)
          (is (spies/called-with? navigate-spy ::history [::some ::routes] ::page nil)))))))

(deftest ^:unit nav-and-replace!-test
  (testing "(nav-and-replace!)"
    (let [navigate-spy (spies/create)]
      (testing "calls navigate* with routes"
        (with-redefs [nav/history ::history
                      nav/routes [::some ::routes]
                      nav/nav-and-replace* navigate-spy]
          (spies/reset! navigate-spy)
          (nav/nav-and-replace! ::page ::params)
          (is (spies/called-with? navigate-spy ::history [::some ::routes] ::page ::params))))
      (testing "defaults params to nil"
        (with-redefs [nav/history ::history
                      nav/routes [::some ::routes]
                      nav/nav-and-replace* navigate-spy]
          (spies/reset! navigate-spy)
          (nav/nav-and-replace! ::page)
          (is (spies/called-with? navigate-spy ::history [::some ::routes] ::page nil)))))))

(defn run-tests []
  (t/run-tests))
