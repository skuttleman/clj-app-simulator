(ns com.ben-allred.app-simulator.ui.views.components.core-test
  (:require
    [clojure.test :as t :refer-macros [are deftest is testing]]
    [com.ben-allred.app-simulator.services.forms.core :as forms]
    [com.ben-allred.app-simulator.templates.views.core :as views]
    [com.ben-allred.app-simulator.templates.views.forms.shared :as shared.views]
    [com.ben-allred.app-simulator.ui.utils.dom :as dom]
    [com.ben-allred.app-simulator.ui.views.components.core :as components]
    [com.ben-allred.app-simulator.utils.chans :as ch]
    [reagent.core :as r]
    [test.utils.dom :as test.dom]
    [test.utils.spies :as spies]))

(defn ^:private available [data]
  {:status :available :data data})

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
    (let [btn [:button.my-button ::with ::content]
          attrs {:open?      true
                 :on-click   ::on-click
                 :class-name "my-class"
                 :items      [{:href ::href-1 :label ::label-1}
                              {:href ::href-2 :label ::label-2}]}
          root (components/menu* attrs btn)]
      (testing "has a dropdown wrapper"
        (is (test.dom/query-one root :.dropdown.my-class))
        (is (-> root
                (test.dom/query-one :.my-button)
                (test.dom/attrs)
                (:on-click)
                (= ::on-click))))

      (testing "has menu items"
        (let [menu (test.dom/query-one root :.dropdown-content)
              [item-1 item-2] (test.dom/query-all menu :.dropdown-item)]
          (is (= ::label-1 (:key (test.dom/attrs item-1))))
          (is (-> item-1
                  (test.dom/query-one :a)
                  (test.dom/attrs)
                  (:href)
                  (= ::href-1)))
          (is (-> item-1
                  (test.dom/query-one :a)
                  (test.dom/contains? ::label-1)))
          (is (= ::label-2 (:key (test.dom/attrs item-2))))
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
            (is (test.dom/query-one button :.fa.fa-angle-up)))))

      (testing "when the menu is closed"
        (let [root (components/menu* (dissoc attrs :open?) btn)]
          (testing "adds an icon to the button"
            (let [button (test.dom/query-one root :.my-button)]
              (is (test.dom/query-one button :.fa.fa-angle-down)))))))))

(deftest ^:unit menu-test
  (testing "(menu)"
    (with-redefs [r/create-class (spies/create :reagent-render)
                  dom/add-listener (spies/constantly ::listener)
                  dom/remove-listener (spies/create)]
      (testing "renders menu* as closed"
        (let [component (components/menu {::some ::attrs} ::button)]
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
                  (is open?)))))))

      (testing "manages a listener"
        (spies/reset! r/create-class dom/add-listener dom/remove-listener)

        (components/menu {::some ::attrs} ::button)
        (is (spies/called-with? dom/add-listener js/window "mouseup" (spies/matcher fn?)))

        (let [{:keys [component-will-unmount]} (ffirst (spies/calls r/create-class))]
          (component-will-unmount ::ignored)
          (is (spies/called-with? dom/remove-listener ::listener)))))))

(deftest ^:unit upload-test
  (testing "(upload)"
    (let [upload (components/upload ::ignored)]
      (with-redefs [forms/syncing? (constantly false)
                    shared.views/with-sync-action (spies/create (fn [attrs _ _] attrs))
                    ch/peek* (spies/constantly ::peek'd)]
        (testing "when rendering the hidden file input"
          (testing "has a class name"
            (is (-> (upload {:class-name ::class})
                    (test.dom/query-one :.file)
                    (test.dom/attrs)
                    (:class-name)
                    (= ::class))))

          (let [on-change-spy (spies/constantly ::change'd)
                root (upload {:on-change on-change-spy :form ::form})
                [_ attrs :as input] (test.dom/query-one root :.file-input)
                event (js/Object.)
                target (js/Object.)]
            (set! (.-target event) target)
            (set! (.-value target) ::value)
            (set! (.-files target) (to-array [::file-1 ::file-2 ::file-3]))

            (testing "has attrs"
              (is (spies/called-with? shared.views/with-sync-action (spies/matcher map?) ::form :on-change))
              (is (= :file (:type attrs)))
              (is (:multiple attrs)))

            (testing "handles :on-change"
              (spies/reset! on-change-spy)
              (let [result ((:on-change (test.dom/attrs input)) event)
                    cb (second (first (spies/calls ch/peek*)))]
                (is (spies/called-times? on-change-spy 1))
                (is (spies/called-with? on-change-spy [::file-1 ::file-2 ::file-3]))
                (is (spies/called-with? ch/peek* ::change'd (spies/matcher fn?)))
                (cb ::ignored)
                (is (nil? (.-files target)))
                (is (nil? (.-value target)))
                (is (= ::peek'd result))))))

        (testing "defaults to multiple files"
          (let [root (upload {})
                [_ attrs] (test.dom/query-one root :.file-input)]
            (is (true? (:multiple attrs)))))

        (let [root (upload {:class-name ::class :static-content ::static-content})
              [_ child] (test.dom/query-one root :.file-label)]
          (testing "displays static-content"
            (is (= child ::static-content)))))

      (testing "and when the button is syncing"
        (with-redefs [forms/syncing? (constantly true)]
          (let [root (upload {:persisting-content ::persisting-content :form ::form})]
            (testing "has no input"
              (is (-> root
                      (test.dom/query-one :input.file-input)
                      (not))))

            (testing "displays persisting-content"
              (test.dom/contains? root ::persisting-content))))))))

(defn run-tests []
  (t/run-tests))
