(ns com.ben-allred.clj-app-simulator.ui.services.navigation-test
  (:require [clojure.test :as t :refer-macros [deftest testing is]]
            [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
            [test.utils.spies :as spies]
            [pushy.core :as pushy]))

(deftest ^:unit navigate*-test
  (testing "(navigate*)"
    (let [pushy-spy (spies/create)
          path-for-spy (spies/constantly ::path)]
      (testing "sets history token"
        (with-redefs [pushy/set-token! pushy-spy
                      nav/path-for path-for-spy]
          (nav/navigate* ::history ::page ::params)
          (is (spies/called-with? path-for-spy ::page ::params))
          (is (spies/called-with? pushy-spy ::history ::path)))))))

(deftest ^:unit nav-and-replace*-test
  (testing "(nav-and-replace*)"
    (let [pushy-spy (spies/create)
          path-for-spy (spies/constantly ::path)]
      (testing "sets history token"
        (with-redefs [pushy/replace-token! pushy-spy
                      nav/path-for path-for-spy]
          (nav/nav-and-replace* ::history ::page ::params)
          (is (spies/called-with? path-for-spy ::page ::params))
          (is (spies/called-with? pushy-spy ::history ::path)))))))

(deftest ^:unit navigate!-test
  (testing "(navigate!)"
    (let [navigate-spy (spies/create)]
      (testing "calls navigate* with routes"
        (with-redefs [nav/history ::history
                      nav/navigate* navigate-spy]
          (spies/reset! navigate-spy)
          (nav/navigate! ::page ::params)
          (is (spies/called-with? navigate-spy ::history ::page ::params))))
      (testing "defaults params to nil"
        (with-redefs [nav/history ::history
                      nav/navigate* navigate-spy]
          (spies/reset! navigate-spy)
          (nav/navigate! ::page)
          (is (spies/called-with? navigate-spy ::history ::page nil)))))))

(deftest ^:unit nav-and-replace!-test
  (testing "(nav-and-replace!)"
    (let [navigate-spy (spies/create)]
      (testing "calls navigate* with routes"
        (with-redefs [nav/history ::history
                      nav/nav-and-replace* navigate-spy]
          (spies/reset! navigate-spy)
          (nav/nav-and-replace! ::page ::params)
          (is (spies/called-with? navigate-spy ::history ::page ::params))))
      (testing "defaults params to nil"
        (with-redefs [nav/history ::history
                      nav/nav-and-replace* navigate-spy]
          (spies/reset! navigate-spy)
          (nav/nav-and-replace! ::page)
          (is (spies/called-with? navigate-spy ::history ::page nil)))))))

(defn run-tests []
  (t/run-tests))
