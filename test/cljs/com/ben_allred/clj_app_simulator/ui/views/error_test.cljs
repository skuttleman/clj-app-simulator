(ns ^:figwheel-load com.ben-allred.clj-app-simulator.ui.views.error-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [com.ben-allred.clj-app-simulator.ui.views.error :as error]
            [test.utils.dom :as test.dom]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.ui.views.main :as main]
            [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]))

(deftest ^:unit not-found-test
  (testing "(not-found)"
    (let [path-for-spy (spies/create (constantly ::href))]
      (with-redefs [nav/path-for path-for-spy]
        (let [not-found (error/not-found nil)]
          (testing "has a header"
            (is (test.dom/query-one not-found main/header)))
          (testing "has a link to the home page"
            (let [link (test.dom/query-one not-found :a.home)]
              (is (= (:href (test.dom/attrs link)) ::href)))))))))
