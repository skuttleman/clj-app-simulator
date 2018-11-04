(ns com.ben-allred.app-simulator.ui.services.navigation-test
  (:require
    [clojure.test :as t :refer-macros [deftest is testing]]
    [com.ben-allred.app-simulator.ui.services.navigation :as nav]
    [pushy.core :as pushy]
    [test.utils.spies :as spies]))

(deftest ^:unit navigate*-test
  (testing "(navigate*)"
    (testing "sets history token"
      (with-redefs [pushy/set-token! (spies/create)
                    nav/path-for (spies/constantly ::path)]
        (nav/navigate* ::history ::page ::params)
        (is (spies/called-with? nav/path-for ::page ::params))
        (is (spies/called-with? pushy/set-token! ::history ::path))))))

(deftest ^:unit nav-and-replace*-test
  (testing "(nav-and-replace*)"
    (testing "sets history token"
      (with-redefs [pushy/replace-token! (spies/create)
                    nav/path-for (spies/constantly ::path)]
        (nav/nav-and-replace* ::history ::page ::params)
        (is (spies/called-with? nav/path-for ::page ::params))
        (is (spies/called-with? pushy/replace-token! ::history ::path))))))

(deftest ^:unit navigate!-test
  (testing "(navigate!)"
    (with-redefs [nav/history ::history
                  nav/navigate* (spies/create)]
      (testing "calls navigate* with routes"
        (spies/reset! nav/navigate*)
        (nav/navigate! ::page ::params)
        (is (spies/called-with? nav/navigate* ::history ::page ::params)))

      (testing "defaults params to nil"
        (spies/reset! nav/navigate*)
        (nav/navigate! ::page)
        (is (spies/called-with? nav/navigate* ::history ::page nil))))))

(deftest ^:unit nav-and-replace!-test
  (testing "(nav-and-replace!)"
    (with-redefs [nav/history ::history
                  nav/nav-and-replace* (spies/create)]
      (testing "calls navigate* with routes"
        (spies/reset! nav/nav-and-replace*)
        (nav/nav-and-replace! ::page ::params)
        (is (spies/called-with? nav/nav-and-replace* ::history ::page ::params)))

      (testing "defaults params to nil"
        (spies/reset! nav/nav-and-replace*)
        (nav/nav-and-replace! ::page)
        (is (spies/called-with? nav/nav-and-replace* ::history ::page nil))))))

(defn run-tests []
  (t/run-tests))
