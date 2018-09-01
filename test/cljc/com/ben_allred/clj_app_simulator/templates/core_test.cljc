(ns com.ben-allred.clj-app-simulator.templates.core-test
  (:require [clojure.test :refer [deftest testing are]]
            [com.ben-allred.clj-app-simulator.templates.core :as templates]))

(defn ^:private ul [value]
  [:ul
   (for [i (range value)]
     [:li i])])

(defn with-child [child]
  [:div.with-child child])

(defn component [x]
  (let [a (inc x)]
    (fn [y]
      (let [b (dec y)]
        (fn [z]
          [:div a b x y z [with-child [ul x]]])))))

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
