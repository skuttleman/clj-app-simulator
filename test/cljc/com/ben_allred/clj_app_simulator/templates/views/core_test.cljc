(ns com.ben-allred.clj-app-simulator.templates.views.core-test
  (:require [clojure.test :as t :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.services.navigation :as nav*]
            [com.ben-allred.clj-app-simulator.templates.core :as templates]
            [com.ben-allred.clj-app-simulator.templates.views.core :as views]
            [test.utils.dom :as test.dom]
            [test.utils.spies :as spies]))

(deftest ^:unit not-found-test
  (testing "(not-found)"
    (let [nav-spy (spies/constantly :HOME)]
      (with-redefs [nav*/path-for nav-spy]
        (let [root (views/not-found ::state)]
          (testing "renders a header"
            (is (test.dom/contains? root [:h1.title.is-2 "Page not found"])))

          (testing "renders a home link"
            (let [link (test.dom/query-one root :.home)]
              (is (-> link
                      (test.dom/attrs)
                      (:href)
                      (= :HOME))))))))))

(deftest ^:unit header-test
  (testing "(header)"
    (let [path-for-spy (spies/constantly "HREF")]
      (with-redefs [nav*/path-for path-for-spy]
        (let [header (views/header {:handler ::handler})]
          (testing "has a home link"
            (is (-> header
                    (test.dom/query-one :.home-link)
                    (test.dom/attrs)
                    (:href)
                    (= "HREF")))
            (is (spies/called-with? path-for-spy :home))))

        (testing "when on the home page"
          (let [header (views/header {:handler :home})]
            (spies/reset! path-for-spy)
            (let [rendered (templates/render header)]
              (testing "has a tab for simulators"
                (is (-> rendered
                        (test.dom/query-one :span.tab)
                        (test.dom/contains? "simulators"))))

              (testing "has a link for resources"
                (let [link (-> rendered
                               (test.dom/query-one :a.tab))]
                  (is (test.dom/contains? rendered "resources"))
                  (is (-> link
                          (test.dom/attrs)
                          (:href)
                          (= "HREF")))
                  (is (spies/called-with? path-for-spy :resources)))))))

        (testing "when on the resources page"
          (let [header (views/header {:handler :resources})]
            (spies/reset! path-for-spy)
            (let [rendered (templates/render header)]
              (testing "has a link for simulators"
                (let [link (-> rendered
                               (test.dom/query-one :a.tab))]
                  (is (test.dom/contains? rendered "simulators"))
                  (is (-> link
                          (test.dom/attrs)
                          (:href)
                          (= "HREF")))
                  (is (spies/called-with? path-for-spy :home))))

              (testing "has a tab for resources"
                (is (-> rendered
                        (test.dom/query-one :span.tab)
                        (test.dom/contains? "resources")))))))))))

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
