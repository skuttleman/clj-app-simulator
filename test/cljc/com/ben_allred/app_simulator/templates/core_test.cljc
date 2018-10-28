(ns com.ben-allred.app-simulator.templates.core-test
  (:require
    [clojure.test :as t :refer [are deftest is testing]]
    [com.ben-allred.app-simulator.templates.core :as templates]))

(defn ^:private ul [value]
  [:ul
   (for [i (range value)]
     [:li i])])

(defn with-child [child]
  [:div.with-child child])

(defn ^:private component [x]
  (let [a (inc x)]
    (fn [y]
      (let [b (dec y)]
        (fn [z]
          [:div a b x y z [with-child [ul x]]])))))

(deftest ^:unit classes-test
  (testing "(classes)"
    (testing "generates classes based on rules"
      (let [result (templates/classes {"class-1" true "class-2" false})]
        (is (= {:class-name "class-1"} result))))
    (testing "appends to existing class-name"
      (let [result (templates/classes {:class-name "existing-class"}
                                      {"class-1" true "class-2" false})]
        (is (= {:class-name "existing-class class-1"} result))))
    (testing "preserves other attributes"
      (let [result (templates/classes {::other ::attrs}
                                      {"class-1" true "class-2" false})]
        (is (= {::other ::attrs :class-name "class-1"}
               result))))
    (testing "does not add :class-name when no classess are added"
      (let [result (templates/classes {::other ::attrs}
                                      {"class-1" false "class-2" false})]
        (is (= {::other ::attrs}
               result))))))

(deftest ^:unit render-test
  (testing "(render)"
    (are [tree expected] (= (templates/render tree) expected)
      nil
      nil

      [:div [:div [:div "thing"]]]
      [:div [:div [:div "thing"]]]

      [ul 2]
      [:ul [[:li 0] [:li 1]]]

      [with-child [:div [:div "child"]]]
      [:div.with-child [:div [:div "child"]]]

      [:span [:p {} [with-child [:span [component 0]]]]]
      [:span [:p {} [:div.with-child [:span [:div 1 -1 0 0 0 [:div.with-child [:ul []]]]]]]]

      [component 3]
      [:div 4 2 3 3 3 [:div.with-child [:ul [[:li 0] [:li 1] [:li 2]]]]])))

(defn run-tests []
  (t/run-tests))
