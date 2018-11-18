(ns com.ben-allred.app-simulator.templates.views.forms.shared-test
  (:require
    #?@(:cljs [[com.ben-allred.app-simulator.ui.simulators.shared.interactions :as interactions]
               [com.ben-allred.app-simulator.ui.utils.dom :as dom]])
    [clojure.test :as t :refer [deftest is testing]]
    [com.ben-allred.app-simulator.services.forms.core :as forms]
    [com.ben-allred.app-simulator.templates.fields :as fields]
    [com.ben-allred.app-simulator.templates.resources.shared :as shared.resources]
    [com.ben-allred.app-simulator.templates.views.core :as views]
    [com.ben-allred.app-simulator.templates.views.forms.shared :as shared.views]
    [com.ben-allred.app-simulator.utils.dates :as dates]
    [test.utils.dom :as test.dom]
    [test.utils.spies :as spies]
    [com.ben-allred.app-simulator.utils.chans :as ch])
  #?(:clj
     (:import
       (clojure.lang IAtom IDeref))))

(deftest ^:unit with-attrs-test
  (testing "(with-attrs)"
    (with-redefs [forms/errors (spies/constantly {:some {:path ::errors}})
                  forms/syncing? (spies/constantly true)
                  forms/touched? (spies/constantly ::touched)
                  forms/tried? (spies/constantly ::tried)]
      (testing "builds attrs"
        (let [swap-spy (spies/create)
              form (reify
                     #?@(:clj  [IAtom
                                (swap [form f a b]
                                  (swap-spy form f a b))
                                IDeref
                                (deref [_]
                                  {:some {:path ::value}})]
                         :cljs [ISwap
                                (-swap! [form f a b]
                                        (swap-spy form f a b))
                                IDeref
                                (-deref [_]
                                        {:some {:path ::value}})]))
              model->view {:some {:path ::to-view}}
              view->model {:some {:path ::to-model}}
              {:keys [disabled errors on-change to-model to-view touched? tried? value]}
              (shared.views/with-attrs {:some ::attrs}
                                       form
                                       [:some :path]
                                       model->view
                                       view->model)]
          (on-change ::new-value)

          (is (spies/called-with? swap-spy form assoc-in [:some :path] ::new-value))
          (is (spies/called-with? forms/syncing? form))
          (is (spies/called-with? forms/errors form))
          (is (spies/called-with? forms/tried? form))

          (is (= ::value value))
          (is (= ::to-view to-view))
          (is (= ::to-model to-model))
          (is (= ::errors errors))
          (is (= ::touched touched?))
          (is (= ::tried tried?))
          (is disabled))))))

(deftest ^:unit create-disabled?-test
  (testing "(create-disabled?)"
    (testing "when the form is valid"
      (with-redefs [forms/valid? (constantly true)
                    forms/tried? (constantly true)]
        (testing "is not disabled"
          (is (not (shared.views/create-disabled? ::form))))))

    (testing "when the form has not been tried"
      (with-redefs [forms/valid? (constantly false)
                    forms/tried? (constantly false)]
        (testing "is not disabled"
          (is (not (shared.views/create-disabled? ::form))))))

    (testing "when the form has been tried and is not valid"
      (with-redefs [forms/valid? (constantly false)
                    forms/tried? (constantly true)]
        (testing "is disabled"
          (is (shared.views/create-disabled? ::form)))))))

(deftest ^:unit edit-disabled?-test
  (testing "(edit-disabled?)"
    (testing "when create is disabled"
      (with-redefs [shared.views/create-disabled? (constantly true)
                    forms/changed? (constantly true)]
        (testing "is disabled"
          (is (shared.views/edit-disabled? ::form)))))

    (testing "when the form has not changed"
      (with-redefs [shared.views/create-disabled? (constantly false)
                    forms/changed? (constantly false)]
        (testing "is disabled"
          (is (shared.views/edit-disabled? ::form)))))

    (testing "when create is not disabled and the form has changed"
      (with-redefs [shared.views/create-disabled? (constantly false)
                    forms/changed? (constantly true)]
        (testing "is not disabled"
          (is (not (shared.views/edit-disabled? ::form))))))))

(deftest ^:unit path-field-test
  (testing "(path-field)"
    (with-redefs [shared.views/with-attrs (spies/create (fn [attrs & _] attrs))]
      (let [[component attrs] (shared.views/path-field ::form ::model->view ::view->model)]
        (is (spies/called-with? shared.views/with-attrs
                                (spies/matcher map?)
                                ::form
                                [:path]
                                ::model->view
                                ::view->model))
        (is (= fields/input component))
        (is (= "Path" (:label attrs)))))))

(deftest ^:unit group-field-test
  (testing "(group-field)"
    (with-redefs [shared.views/with-attrs (spies/create (comp first list))]
      (let [root (shared.views/group-field ::form ::model->view ::view->model)
            input (test.dom/query-one root fields/input)]
        (testing "has an input with attrs"
          (is (spies/called-with? shared.views/with-attrs
                                  (spies/matcher map?)
                                  ::form
                                  [:group]
                                  ::model->view
                                  ::view->model))
          (is (= "Group" (:label (test.dom/attrs input)))))))))

(deftest ^:unit description-field-test
  (testing "(description-field)"
    (with-redefs [shared.views/with-attrs (spies/create (comp first list))]
      (let [root (shared.views/description-field ::form ::model->view ::view->model)
            input (test.dom/query-one root fields/textarea)]
        (testing "has an input with attrs"
          (is (spies/called-with? shared.views/with-attrs
                                  (spies/matcher map?)
                                  ::form
                                  [:description]
                                  ::model->view
                                  ::view->model))
          (is (= "Description" (:label (test.dom/attrs input)))))))))

(deftest ^:unit status-field-test
  (testing "(status-field)"
    (with-redefs [shared.views/with-attrs (spies/create (fn [v & _] (assoc v :more ::attrs)))]
      (testing "renders the form field"
        (let [[node attrs resource] (shared.views/status-field ::form ::model->view ::view->model)]
          (is (spies/called-with? shared.views/with-attrs
                                  (spies/matcher any?)
                                  ::form
                                  [:response :status]
                                  ::model->view
                                  ::view->model))
          (is (= fields/select node))
          (is (= "Status" (:label attrs)))
          (is (= ::attrs (:more attrs)))
          (is (= shared.resources/statuses resource)))))))

(deftest ^:unit delay-field-test
  (testing "(delay-field)"
    (with-redefs [shared.views/with-attrs (spies/create (fn [v & _] (assoc v :more ::attrs)))]
      (testing "renders the form field"
        (let [[node attrs] (shared.views/delay-field ::form ::model->view ::view->model)]
          (is (spies/called-with? shared.views/with-attrs
                                  (spies/matcher any?)
                                  ::form
                                  [:delay]
                                  ::model->view
                                  ::view->model))
          (is (= fields/input node))
          (is (= "Delay (ms)" (:label attrs)))
          (is (= ::attrs (:more attrs))))))))

(deftest ^:unit headers-field-test
  (testing "(headers-field)"
    (with-redefs [shared.views/with-attrs (spies/create (fn [v & _] (assoc v :more ::attrs)))]
      (testing "renders the form field"
        (let [[node attrs header] (shared.views/headers-field ::form ::model->view ::view->model)]
          (is (spies/called-with? shared.views/with-attrs
                                  (spies/matcher any?)
                                  ::form
                                  [:response :headers]
                                  ::model->view
                                  ::view->model))
          (is (= fields/multi node))
          (is (= fields/header header))
          (is (= "Headers" (:label attrs)))
          (is (= ::attrs (:more attrs)))
          (is (= "header-1" ((:key-fn attrs) [1 :value])))
          (is (= ["" ""] ((:new-fn attrs) ::ignored))))))))

(deftest ^:unit method-field-test
  (testing "(method-field)"
    (with-redefs [shared.views/with-attrs (spies/create (fn [v & _] (assoc v :more ::attrs)))]
      (testing "renders the form field"
        (let [[node attrs resource] (shared.views/method-field ::form ::resource ::model->view ::view->model)]
          (is (spies/called-with? shared.views/with-attrs
                                  (spies/matcher any?)
                                  ::form
                                  [:method]
                                  ::model->view
                                  ::view->model))
          (is (= fields/select node))
          (is (= "HTTP Method" (:label attrs)))
          (is (true? (:auto-focus? attrs)))
          (is (= ::attrs (:more attrs)))
          (is (= ::resource resource)))))))

(deftest ^:unit sim-request-test
  (testing "(sim-request)"
    (with-redefs [#?@(:cljs [interactions/show-request-modal (spies/constantly ::click)])
                  dates/format (spies/constantly ::formatted)]
      (let [request {:timestamp ::timestamp :details ::details}
            root (shared.views/sim-request ::sim request)
            tree (test.dom/query-one root :.request)]

        (testing "displays formatted timestamp"
          (is (spies/called-with? dates/format ::timestamp))
          (is (test.dom/contains? tree ::formatted)))

        #?(:cljs
           (testing "has a click handler"
             (is (-> tree
                     (test.dom/attrs)
                     (:on-click)
                     (= ::click)))
             (is (spies/called-with? interactions/show-request-modal ::sim request))))))))

(deftest ^:unit with-sync-action-test
  (testing "(with-sync-action)"
    (with-redefs [#?@(:cljs [dom/prevent-default (spies/create)])
                  ch/peek (spies/constantly ::peek'd)
                  forms/try! (spies/create)
                  forms/valid? (spies/constantly true)
                  forms/sync! (spies/create)
                  forms/ready! (spies/create)]
      (let [handler-spy (spies/constantly ::result)]
        (testing "returns an attrs map"
          (let [attrs (shared.views/with-sync-action {:some :attrs :go :here} ::form :on-event)]
            (is (= {:some :attrs :go :here} (select-keys attrs #{:some :go})))))

        (testing "when the attrs are :disabled"
          (spies/reset! handler-spy forms/try! forms/valid? forms/sync! forms/ready! #?(:cljs dom/prevent-default))
          (let [handler (-> {:on-event handler-spy :disabled true}
                            (shared.views/with-sync-action ::form :on-event)
                            (:on-event))]
            (handler ::event)
            #?(:cljs
               (testing "prevents default behavior"
                 (is (spies/called-with? dom/prevent-default ::event))))

            (testing "does not try the form"
              (is (spies/never-called? forms/try!)))

            (testing "does not invoke the handler"
              (is (spies/never-called? handler-spy)))))

        (testing "when the form is not valid"
          (spies/reset! handler-spy forms/try! forms/valid? forms/sync! forms/ready! #?(:cljs dom/prevent-default))
          (spies/returning! forms/valid? false)
          (let [handler (-> {:on-event handler-spy}
                            (shared.views/with-sync-action ::form :on-event)
                            (:on-event))]
            (handler ::event)
            #?(:cljs
               (testing "prevents default behavior"
                 (is (spies/called-with? dom/prevent-default ::event))))

            (testing "tries the form"
              (is (spies/called-with? forms/try! ::form))
              (is (spies/called-with? forms/valid? ::form)))

            (testing "does not invoke the handler"
              (is (spies/never-called? handler-spy)))))

        (testing "when the form is sync-able"
          (spies/reset! handler-spy forms/try! forms/valid? forms/sync! forms/ready! #?(:cljs dom/prevent-default))
          (let [handler (-> {:on-event handler-spy}
                            (shared.views/with-sync-action ::form :on-event)
                            (:on-event))
                result (handler ::event)
                [_ f] (first (spies/calls ch/peek))]
            (testing "handles the event"
              (is (spies/called-with? forms/sync! ::form))
              (is (spies/called-with? handler-spy ::event))
              (is (spies/called-with? ch/peek ::result (spies/matcher fn?)))
              (f [:status ::async])
              (is (spies/called-with? forms/ready! ::form [:status ::async]))
              (is (= ::peek'd result)))))))))

(deftest ^:unit sync-button-test
  (testing "(sync-button)"
    (testing "when the button is syncing"
      (with-redefs [forms/syncing? (spies/constantly ::syncing)]
        (let [button (-> {:form ::form
                          :text ::text
                          :sync-text ::sync-text}
                         (shared.views/sync-button)
                         (test.dom/query-one :.sync-button))
              attrs (test.dom/attrs button)]
          (testing "renders the button in syncing state"
            (is (spies/called-with? forms/syncing? ::form))
            (is (= ::syncing (:disabled attrs)))
            (is (test.dom/contains? button ::sync-text))
            (is (not (test.dom/contains? button ::text)))
            (is (test.dom/contains? button [views/spinner]))))))

    (testing "when the button is not syncing"
      (with-redefs [forms/syncing? (spies/constantly false)]
        (let [button (-> {:form ::form
                          :text ::text
                          :sync-text ::sync-text
                          :disabled ::disabled}
                         (shared.views/sync-button)
                         (test.dom/query-one :.sync-button))
              attrs (test.dom/attrs button)]
          (testing "renders the button in non-syncing state"
            (is (= ::disabled (:disabled attrs)))
            (is (test.dom/contains? button ::text))
            (is (not (test.dom/contains? button ::sync-text)))
            (is (not (test.dom/contains? button [views/spinner])))))))

    (testing "when the button has :on-event attr"
      (with-redefs [forms/syncing? (spies/create)
                    shared.views/with-sync-action (spies/constantly {::some ::attrs})]
        (let [attrs (-> {:form ::form
                         :on-event ::on-event
                         :sync-text ::sync-text
                         :text ::text}
                        (shared.views/sync-button)
                        (test.dom/query-one :.sync-button)
                        (test.dom/attrs))]
          (testing "calls with-sync-action"
            (is (spies/called-with? shared.views/with-sync-action
                                    (spies/matcher (comp empty? #(select-keys % #{:form :on-event :syn-text :text})))
                                    ::form
                                    ::on-event))
            (is (= attrs {::some ::attrs}))))))))

(defn run-tests []
  (t/run-tests))
