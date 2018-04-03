(ns ^:figwheel-load com.ben-allred.clj-app-simulator.ui.views.main-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [com.ben-allred.clj-app-simulator.ui.views.main :as main]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
            [test.utils.dom :as test.dom]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.ui.views.simulators :as sims]))

(deftest ^:unit header-test
  (testing "(header)"
    (let [path-for-spy (spies/create (constantly ::href))]
      (with-redefs [nav/path-for path-for-spy]
        (let [header (main/header)]
          (testing "has link to home page"
            (let [link (test.dom/query-one header :a.home-link)
                  logo (test.dom/query-one link :img.logo)]
              (is (= ::href (:href (test.dom/attrs link))))
              (is (= "/images/logo.png" (:src (test.dom/attrs logo)))))

            (testing "has page title"
              (let [h1 (test.dom/query-one header :h1)]
                (is (test.dom/contains? h1 "App Simulator"))))))))))

(deftest ^:unit root-test
  (testing "(root)"
    (let [root (main/root {:simulators ::simulators})]
      (testing "has a header"
        (is (= "Simulators" (second (test.dom/query-one root :h2)))))
      (testing "displays simulators"
        (is (= [sims/simulators ::simulators]
               (test.dom/query-one root sims/simulators)))))))
