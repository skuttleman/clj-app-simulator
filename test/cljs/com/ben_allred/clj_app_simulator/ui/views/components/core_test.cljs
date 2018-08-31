(ns com.ben-allred.clj-app-simulator.ui.views.components.core-test
  (:require [cljs.test :as t :refer-macros [deftest testing is are]]
            [test.utils.dom :as test.dom]
            [com.ben-allred.clj-app-simulator.ui.views.components.core :as components]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.ui.utils.dom :as dom]
            [com.ben-allred.clj-app-simulator.templates.views.core :as views]))

(defn ^:private available [data]
  {:status :available :data data})

(deftest ^:unit with-height-test
  (testing "(with-height)"
    (let [are-f (fn [open? item-count]
                  (-> {}
                      (components/with-height open? item-count)
                      (:style)
                      (:height)
                      (js/parseInt)))]
      (testing "calculates item height when open"
        (are [item-count expected] (= expected (are-f true item-count))
          0 24
          1 42
          2 60
          10 204
          25 474))

      (testing "calculates item height when closed"
        (doseq [num (repeat 10 (rand-int 1000))]
          (is (zero? (are-f false num))))))))

(deftest ^:unit spinner-overlay-test
  (testing "(spinner-overlay)"
    (testing "when shown"
      (let [spinner (components/spinner-overlay true ::component)]
        (testing "has spinner component"
          (let [node (test.dom/query-one spinner :.spinner-container)]
            (test.dom/contains? node views/spinner)))
        (testing "has passed component"
          (let [node (test.dom/query-one spinner :.component-container)]
            (test.dom/contains? node ::component)))))
    (testing "when not show"
      (let [spinner (components/spinner-overlay false ::component)]
        (testing "does not have spinner component"
          (is (not (test.dom/contains? spinner views/spinner))))
        (testing "does not have passed component"
          (is (not (test.dom/contains? spinner ::component))))))))

(deftest ^:unit with-status-test
  (testing "(with-status)"
    (testing "when component is vector"
      (let [root (components/with-status [::component ::arg] {:status :available :data {::some ::item}})]
        (testing "renders the partialed component"
          (is (= [::component ::arg {::some ::item}]
                 (test.dom/query-one root ::component))))))

    (testing "when status is :available"
      (let [root (components/with-status ::component {:status :available :data {::some ::item}})]
        (testing "renders the component"
          (is (= [::component {::some ::item}]
                 (test.dom/query-one root ::component))))))

    (testing "when status is :available for all items"
      (let [root (components/with-status ::component (available ::data1) (available ::data2) (available ::data3))]
        (testing "renders the component"
          (is (= [::component ::data1 ::data2 ::data3]
                 (test.dom/query-one root ::component))))))

    (testing "when status is not :available for at least one item"
      (let [root (components/with-status ::component (available ::data1) {:status :bad :data ::data2} (available ::data3))]
        (testing "does not render the component"
          (is (nil? (test.dom/query-one root ::component))))))

    (testing "when the status is not :available"
      (let [root (components/with-status ::component {:status ::random-status :data nil})]
        (testing "renders a spinner"
          (is (test.dom/query-one root views/spinner)))))))

(deftest ^:unit menu*-test
  (testing "(menu*)"
    (let [height-spy (spies/constantly {::some ::attrs})]
      (with-redefs [components/with-height height-spy]
        (let [btn [:button.my-button ::with ::content]
              attrs {:open?      true
                     :on-click   ::on-click
                     :class-name :my-class
                     :items      [{:href ::href-1 :label ::label-1}
                                  {:href ::href-2 :label ::label-2}]}
              root (components/menu* attrs btn)]
          (testing "has a dropdown menu wrapper"
            (is (-> root
                    (test.dom/query-one :.dropdown-menu-wrapper.my-class)
                    (test.dom/attrs)
                    (:on-click)
                    (= ::on-click))))

          (testing "has menu items"
            (let [menu (test.dom/query-one root :.menu)
                  [item-1 item-2] (test.dom/query-all menu :.menu-item)]
              (is (= 0 (:key (test.dom/attrs item-1))))
              (is (-> item-1
                      (test.dom/query-one :a)
                      (test.dom/attrs)
                      (:href)
                      (= ::href-1)))
              (is (-> item-1
                      (test.dom/query-one :a)
                      (test.dom/contains? ::label-1)))
              (is (= 1 (:key (test.dom/attrs item-2))))
              (is (-> item-2
                      (test.dom/query-one :a)
                      (test.dom/attrs)
                      (:href)
                      (= ::href-2)))
              (is (-> item-2
                      (test.dom/query-one :a)
                      (test.dom/contains? ::label-2)))))

          (testing "when the menu is open"
            (testing "adds an icon to the button"
              (let [button (test.dom/query-one root :.my-button)]
                (is (test.dom/query-one button :.dropdown-chevron.fa.fa-chevron-up))))

            (testing "has a dropdown menu"
              (let [dropdown-menu (test.dom/query-one root :.dropdown-menu.open)]
                (is (spies/called-with? height-spy
                                        (spies/matcher map?)
                                        (spies/matcher identity)
                                        2))
                (is (-> dropdown-menu
                        (test.dom/attrs)
                        (::some)
                        (= ::attrs))))))

          (testing "when the menu is closed"
            (let [root (components/menu* (dissoc attrs :open?) btn)]
              (testing "adds an icon to the button"
                (let [button (test.dom/query-one root :.my-button)]
                  (is (test.dom/query-one button :.dropdown-chevron.fa.fa-chevron-down))))

              (testing "has a dropdown menu"
                (let [dropdown-menu (test.dom/query-one root :.dropdown-menu.closed)]
                  (is (spies/called-with? height-spy
                                          (spies/matcher map?)
                                          (spies/matcher not)
                                          2))
                  (is (-> dropdown-menu
                          (test.dom/attrs)
                          (::some)
                          (= ::attrs))))))))))))

(deftest ^:unit menu-test
  (testing "(menu)"
    (let [component (components/menu {::some ::attrs} ::button)]
      (testing "renders menu* as closed"
        (let [root (component {::some ::attrs} ::button)
              [component* attrs button] root]
          (is (= component* components/menu*))
          (is (= ::attrs (::some attrs)))
          (is (not (:open? attrs)))
          (is (= ::button button))

          (testing "when clicking on the menu"
            (-> root
                (test.dom/query-one components/menu*)
                (test.dom/simulate-event :click))
            (let [root (component {::some ::attrs} ::button)
                  [_ {:keys [open?]}] root]
              (testing "renders menu* as open"
                (is open?)))))))))

(deftest ^:unit upload-test
  (testing "(upload)"
    (with-redefs [gensym (spies/constantly ::id)]
      (testing "when rendering the hidden file input"
        (let [on-change-spy (spies/create)
              root (components/upload nil)
              tree (root {:on-change on-change-spy :multiple ::multiple})
              [_ attrs :as input] (test.dom/query-one tree :.hidden.file-upload)
              event (js/Object.)
              target (js/Object.)]
          (set! (.-target event) target)
          (set! (.-value target) ::value)
          (set! (.-files target) (to-array [::file-1 ::file-2 ::file-3]))

          (testing "has attrs"
            (is (= ::id (:id attrs)))
            (is (= :file (:type attrs)))
            (is (= ::multiple (:multiple attrs))))

          (testing "handles :on-change"
            (spies/reset! on-change-spy)
            (test.dom/simulate-event input :change event)

            (is (spies/called-times? on-change-spy 1))
            (is (spies/called-with? on-change-spy [::file-1 ::file-2 ::file-3]))
            (is (nil? (.-files target)))
            (is (nil? (.-value target))))))

      (testing "defaults to multiple files"
        (let [root (components/upload nil)
              tree (root {})
              [_ attrs] (test.dom/query-one tree :.hidden.file-upload)]
          (is (true? (:multiple attrs)))))

      (testing "renders a button"
        (let [query-spy (spies/constantly ::node)
              click-spy (spies/create)
              root (components/upload nil)
              tree (root {:class-name ::class} ::child-1 ::child-2)
              [_ attrs child-1 child-2 :as button] (test.dom/query-one tree :button)]
          (testing "has attrs"
            (is (= ::class (:class-name attrs))))

          (testing "handles :on-click"
            (with-redefs [dom/query-one query-spy
                          dom/click click-spy]
              (test.dom/simulate-event button :click)

              (is (spies/called-with? query-spy (str "#" ::id)))
              (is (spies/called-with? click-spy ::node))))

          (testing "has children"
            (is (= ::child-1 child-1))
            (is (= ::child-2 child-2))))))))

(defn run-tests []
  (t/run-tests))
