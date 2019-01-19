(ns com.ben-allred.app-simulator.templates.views.core-test
  (:require
    [clojure.test :as t :refer [deftest is testing]]
    [com.ben-allred.app-simulator.services.navigation :as nav*]
    [com.ben-allred.app-simulator.templates.core :as templates]
    [com.ben-allred.app-simulator.templates.views.core :as views]
    [test.utils.dom :as test.dom]
    [test.utils.spies :as spies]
    [com.ben-allred.app-simulator.utils.logging :as log]))

(deftest ^:unit not-found-test
  (testing "(not-found)"
    (with-redefs [nav*/path-for (constantly :HOME)]
      (let [root (views/not-found ::state)]
        (testing "renders a header"
          (is (test.dom/contains? root [:h1.title.is-2 "Page not found"])))

        (testing "renders a home link"
          (let [link (test.dom/query-one root :.home)]
            (is (-> link
                    (test.dom/attrs)
                    (:href)
                    (= :HOME)))))))))

(deftest ^:unit header-test
  (testing "(header)"
    (with-redefs [nav*/path-for name]
      (let [header (views/header {:handler ::handler})
            rendered (templates/render header)
            [tab-1 tab-2 tab-3 :as tabs] (-> rendered
                                             (test.dom/query-all :.tab))]
        (is (= 3 (count tabs)))

        (testing "has a home link"
          (is (test.dom/query-one tab-1 :.home-link))
          (is (-> tab-1
                  (test.dom/query-one :a)
                  (test.dom/attrs)
                  (:href)
                  (= "home")))
          (is (test.dom/query-one tab-1 :span.logo)))

        (testing "has a simulators link"
          (is (-> tab-2
                  (test.dom/query-one :a)
                  (test.dom/attrs)
                  (:href)
                  (= "home")))
          (is (test.dom/contains? tab-2 "Simulators")))

        (testing "has a resources link"
          (is (-> tab-3
                  (test.dom/query-one :a)
                  (test.dom/attrs)
                  (:href)
                  (= "resources")))
          (is (test.dom/contains? tab-3 "Resources")))))))

(deftest ^:unit root-test
  (testing "(root)"
    (let [children [[:div.child-1 "child-1"]
                    [:div.child-2 "child-2"]
                    [:div.child-3 "child-3"]]
          root (apply views/root children)]
      (testing "contains a header"
        (is (-> root
                (test.dom/query-one :h1)
                (test.dom/contains? "Simulators"))))

      (testing "renders children"
        (is (-> root
                (test.dom/query-one :.child-1)
                (test.dom/contains? "child-1")))
        (is (-> root
                (test.dom/query-one :.child-2)
                (test.dom/contains? "child-2")))
        (is (-> root
                (test.dom/query-one :.child-3)
                (test.dom/contains? "child-3")))))))

(deftest ^:unit details-test
  (testing "(details)"
    (let [child [:div.child "child"]
          root (views/details child)]
      (testing "contains a header"
        (is (-> root
                (test.dom/query-one :h1)
                (test.dom/contains? "Simulator Details"))))

      (testing "renders the child"
        (is (-> root
                (test.dom/query-one :.child)
                (test.dom/contains? "child")))))))

(deftest ^:unit new-test
  (testing "(new)"
    (let [child [:div.child "child"]
          root (views/new {:page {:query-params {:type "type"}}} child)]
      (testing "contains a header"
        (is (-> root
                (test.dom/query-one :h1)
                (test.dom/contains? "New TYPE Simulator"))))

      (testing "renders the child"
        (is (-> root
                (test.dom/query-one :.child)
                (test.dom/contains? "child")))))))

(deftest ^:unit resources-test
  (testing "(resources)"
    (let [child [:div.child "child"]
          root (views/resources child)]
      (testing "contains a header"
        (is (-> root
                (test.dom/query-one :h1)
                (test.dom/contains? "Resources"))))

      (testing "renders the child"
        (is (-> root
                (test.dom/query-one :.child)
                (test.dom/contains? "child")))))))

(deftest ^:unit app*-test
  (testing "(app*)"
    (let [components-spy (spies/constantly :component#component)
          state {:page {:handler ::handler} :more ::state}
          root (views/app* {:toast      :toast#toast
                            :modal      :modal#modal
                            :components components-spy} state)
          app (test.dom/query-one root :.app)]
      (testing "contains toast"
        (is (-> app
                (test.dom/query-one :#toast)
                (= [:toast#toast state]))))

      (testing "contains modal"
        (is (-> app
                (test.dom/query-one :#modal)
                (= [:modal#modal state]))))

      (testing "when rendering the scrollable container"
        (let [scrollable (test.dom/query-one app :.scrollable)]
          (testing "contains a header"
            (is (-> scrollable
                    (test.dom/query-one views/header)
                    (= [views/header {:handler ::handler}]))))

          (testing "renders the component"
            (is (-> scrollable
                    (test.dom/query-one :.main)
                    (test.dom/query-one :#component)
                    (= [:component#component state])))
            (is (spies/called-with? components-spy ::handler views/not-found))))))))

(defn run-tests []
  (t/run-tests))
