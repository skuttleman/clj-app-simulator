(ns com.ben-allred.clj-app-simulator.ui.services.forms.fields-test
  (:require [cljs.test :as t :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.ui.services.forms.fields :as fields]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.ui.utils.dom :as dom]
            [test.utils.dom :as test.dom]))

(defn ^:private render [component & args]
  (let [[ff & more] (apply component args)]
    (apply ff more)))

(deftest ^:unit select-test
  (testing "(select)"
    (let [to-view (spies/create #(assoc % :to-view true))
          to-model (spies/create #(assoc % :to-model true))
          on-change (spies/create)
          target-value (spies/create first)]
      (with-redefs [dom/target-value target-value]
        (testing "when rendering a select component"
          (let [options [[{:a 1} "A"]
                         [{:b 2} "B"]
                         [{:c 3} "C"]]
                attrs {:to-view    to-view
                       :to-model   to-model
                       :on-change  on-change
                       :value      {:some ::value}
                       :class-name ::class-name
                       :label      ::label
                       :errors     {:has :errors}}
                root (render fields/select attrs options)
                form-field (test.dom/query-one root :.form-field)]
            (testing "renders a form-field"
              (is (-> form-field
                      (test.dom/attrs)
                      (:class-name)
                      (= :errors))))

            (testing "renders a :label element"
              (is (-> form-field
                      (test.dom/query-one :label)
                      (test.dom/contains? ::label))))

            (let [select (test.dom/query-one form-field :select)
                  attrs (test.dom/attrs select)]
              (testing "renders a :select element"
                (is (spies/called-with? to-view {:some ::value}))
                (is (= {:some ::value :to-view true} (:value attrs)))
                (is (= ::class-name (:class-name attrs))))

              (testing "handles :on-change event"
                (spies/reset! on-change to-model target-value)
                (test.dom/simulate-event select :change [{:new :value}])
                (is (spies/called-with? target-value [{:new :value}]))
                (is (spies/called-with? to-model {:new :value}))
                (is (spies/called-with? on-change {:new :value :to-model true}))))

            (testing "renders the :option elements"
              (let [option-elements (test.dom/query-all form-field :option)]
                (is (= 3 (count option-elements)))
                (doseq [[idx elem] (map-indexed vector option-elements)
                        :let [attrs (test.dom/attrs elem)
                              [option label] (nth options idx nil)
                              option (assoc option :to-view true)]]
                  (is (= (str option) (:key attrs)))
                  (is (= option (:value attrs)))
                  (is (test.dom/contains? elem label)))))

            (testing "and when rendering without a label"
              (let [root (render fields/select (dissoc attrs :label) options)
                    form-field (test.dom/query-one root :.form-field)]
                (testing "does not render a :label element"
                  (is (not (test.dom/query-one form-field :label))))))

            (testing "and when rendering without errors"
              (let [root (render fields/select (dissoc attrs :errors) options)
                    form-field (test.dom/query-one root :.form-field)]
                (testing "does not have an :errors class"
                  (is (not (:class-name (test.dom/attrs form-field)))))))

            (testing "and when :to-view is nil"
              (let [root (render fields/select (dissoc attrs :to-view) options)
                    form-field (test.dom/query-one root :.form-field)
                    select (test.dom/query-one form-field :select)]
                (testing "has the unchanged :value"
                  (is (= {:some ::value} (:value (test.dom/attrs select)))))))

            (testing "and when :to-model is nil"
              (let [root (render fields/select (dissoc attrs :to-model) options)
                    form-field (test.dom/query-one root :.form-field)
                    select (test.dom/query-one form-field :select)]
                (testing "passes the target value to :on-change"
                  (spies/reset! on-change)
                  (test.dom/simulate-event select :change [{:new :value}])
                  (is (spies/called-with? on-change {:new :value})))))))))))

(deftest ^:unit textarea-test
  (testing "(textarea)"
    (let [to-view (spies/create #(assoc % :to-view true))
          to-model (spies/create #(assoc % :to-model true))
          on-change (spies/create)
          target-value (spies/create first)]
      (with-redefs [dom/target-value target-value]
        (testing "when rendering a textarea component"
          (let [attrs {:to-view    to-view
                       :to-model   to-model
                       :on-change  on-change
                       :value      {:some ::value}
                       :class-name ::class-name
                       :label      ::label
                       :errors     {:has :errors}}
                root (render fields/textarea attrs)
                form-field (test.dom/query-one root :.form-field)]
            (testing "renders a form-field"
              (is (-> form-field
                      (test.dom/attrs)
                      (:class-name)
                      (= :errors))))

            (testing "renders a :label element"
              (is (-> form-field
                      (test.dom/query-one :label)
                      (test.dom/contains? ::label))))

            (let [textarea (test.dom/query-one form-field :textarea)
                  attrs (test.dom/attrs textarea)]
              (testing "renders a :textarea element"
                (is (spies/called-with? to-view {:some ::value}))
                (is (= {:some ::value :to-view true} (:value attrs)))
                (is (= ::class-name (:class-name attrs))))

              (testing "handles :on-change event"
                (spies/reset! on-change to-model target-value)
                (test.dom/simulate-event textarea :change [{:new :value}])
                (is (spies/called-with? target-value [{:new :value}]))
                (is (spies/called-with? to-model {:new :value}))
                (is (spies/called-with? on-change {:new :value :to-model true}))))

            (testing "and when rendering without a label"
              (let [root (render fields/textarea (dissoc attrs :label))
                    form-field (test.dom/query-one root :.form-field)]
                (testing "does not render a :label element"
                  (is (not (test.dom/query-one form-field :label))))))

            (testing "and when rendering without errors"
              (let [root (render fields/textarea (dissoc attrs :errors))
                    form-field (test.dom/query-one root :.form-field)]
                (testing "does not have an :errors class"
                  (is (not (:class-name (test.dom/attrs form-field)))))))

            (testing "and when :to-view is nil"
              (let [root (render fields/textarea (dissoc attrs :to-view))
                    form-field (test.dom/query-one root :.form-field)
                    textarea (test.dom/query-one form-field :textarea)]
                (testing "has the unchanged :value"
                  (is (= {:some ::value} (:value (test.dom/attrs textarea)))))))

            (testing "and when :to-model is nil"
              (let [root (render fields/textarea (dissoc attrs :to-model))
                    form-field (test.dom/query-one root :.form-field)
                    textarea (test.dom/query-one form-field :textarea)]
                (testing "passes the target value to :on-change"
                  (spies/reset! on-change)
                  (test.dom/simulate-event textarea :change [{:new :value}])
                  (is (spies/called-with? on-change {:new :value})))))))))))

(deftest ^:unit input-test
  (testing "(input)"
    (let [to-view (spies/create #(assoc % :to-view true))
          to-model (spies/create #(assoc % :to-model true))
          on-change (spies/create)
          target-value (spies/create first)]
      (with-redefs [dom/target-value target-value]
        (testing "when rendering a input component"
          (let [attrs {:to-view    to-view
                       :to-model   to-model
                       :on-change  on-change
                       :value      {:some ::value}
                       :class-name ::class-name
                       :label      ::label
                       :errors     {:has :errors}
                       :type       ::type}
                root (render fields/input attrs)
                form-field (test.dom/query-one root :.form-field)]
            (testing "renders a form-field"
              (is (-> form-field
                      (test.dom/attrs)
                      (:class-name)
                      (= :errors))))

            (testing "renders a :label element"
              (is (-> form-field
                      (test.dom/query-one :label)
                      (test.dom/contains? ::label))))

            (let [input (test.dom/query-one form-field :input)
                  attrs (test.dom/attrs input)]
              (testing "renders a :input element"
                (is (spies/called-with? to-view {:some ::value}))
                (is (= {:some ::value :to-view true} (:value attrs)))
                (is (= ::class-name (:class-name attrs)))
                (is (= ::type (:type attrs))))

              (testing "handles :on-change event"
                (spies/reset! on-change to-model target-value)
                (test.dom/simulate-event input :change [{:new :value}])
                (is (spies/called-with? target-value [{:new :value}]))
                (is (spies/called-with? to-model {:new :value}))
                (is (spies/called-with? on-change {:new :value :to-model true}))))

            (testing "and when rendering without a label"
              (let [root (render fields/input (dissoc attrs :label))
                    form-field (test.dom/query-one root :.form-field)]
                (testing "does not render a :label element"
                  (is (not (test.dom/query-one form-field :label))))))

            (testing "and when rendering without errors"
              (let [root (render fields/input (dissoc attrs :errors))
                    form-field (test.dom/query-one root :.form-field)]
                (testing "does not have an :errors class"
                  (is (not (:class-name (test.dom/attrs form-field)))))))

            (testing "and when :type is nil"
              (let [root (render fields/input (dissoc attrs :type))
                    form-field (test.dom/query-one root :.form-field)
                    input (test.dom/query-one form-field :input)]
                (testing "has a :type of :text"
                  (is (= :text (:type (test.dom/attrs input)))))))

            (testing "and when :to-view is nil"
              (let [root (render fields/input (dissoc attrs :to-view))
                    form-field (test.dom/query-one root :.form-field)
                    input (test.dom/query-one form-field :input)]
                (testing "has the unchanged :value"
                  (is (= {:some ::value} (:value (test.dom/attrs input)))))))

            (testing "and when :to-model is nil"
              (let [root (render fields/input (dissoc attrs :to-model))
                    form-field (test.dom/query-one root :.form-field)
                    input (test.dom/query-one form-field :input)]
                (testing "passes the target value to :on-change"
                  (spies/reset! on-change)
                  (test.dom/simulate-event input :change [{:new :value}])
                  (is (spies/called-with? on-change {:new :value})))))))))))

(deftest ^:unit header-test
  (testing "(header)"
    (let [to-view (spies/create (constantly [::view-key ::view-value]))
          to-model (spies/create (constantly ::model))
          on-change (spies/create)
          target-value (spies/create first)]
      (with-redefs [dom/target-value target-value]
        (testing "when rendering a header component"
          (let [attrs {:to-view   to-view
                       :to-model  to-model
                       :on-change on-change
                       :value     [::model-key ::model-value]
                       :label     ::label
                       :errors    [:has :errors]}
                root (render fields/header attrs)
                form-field (test.dom/query-one root :.form-field)]
            (testing "renders a form-field"
              (is (-> form-field
                      (test.dom/attrs)
                      (:class-name)
                      (= :errors))))

            (testing "renders a :label element"
              (is (-> form-field
                      (test.dom/query-one :label)
                      (test.dom/contains? ::label))))

            (let [header (test.dom/query-one form-field :input.header)
                  value (test.dom/query-one form-field :input.value)
                  header-attrs (test.dom/attrs header)
                  value-attrs (test.dom/attrs value)]
              (testing "renders a :header element"
                (is (spies/called-with? to-view [::model-key ::model-value]))
                (is (= ::view-key (:value header-attrs)))
                (is (= ::view-value (:value value-attrs))))

              (testing "handles :on-change event for .header"
                (spies/reset! on-change to-model target-value)
                (test.dom/simulate-event header :change [::new-header])
                (is (spies/called-with? target-value [::new-header]))
                (is (spies/called-with? to-model [::new-header ::view-value]))
                (is (spies/called-with? on-change ::model)))

              (testing "handles :on-change event for .value"
                (spies/reset! on-change to-model target-value)
                (test.dom/simulate-event value :change [::new-header])
                (is (spies/called-with? target-value [::new-header]))
                (is (spies/called-with? to-model [::view-key ::new-header]))
                (is (spies/called-with? on-change ::model))))

            (testing "and when rendering without a label"
              (let [root (render fields/header (dissoc attrs :label))
                    form-field (test.dom/query-one root :.form-field)]
                (testing "does not render a :label element"
                  (is (not (test.dom/query-one form-field :label))))))

            (testing "and when rendering without errors"
              (let [root (render fields/header (dissoc attrs :errors))
                    form-field (test.dom/query-one root :.form-field)]
                (testing "does not have an :errors class"
                  (is (not (:class-name (test.dom/attrs form-field)))))))

            (testing "and when :to-view is nil"
              (let [root (render fields/header (dissoc attrs :to-view))
                    form-field (test.dom/query-one root :.form-field)
                    header (test.dom/query-one form-field :.header)
                    value (test.dom/query-one form-field :.value)]
                (testing "has the unchanged :value"
                  (is (= ::model-key (:value (test.dom/attrs header))))
                  (is (= ::model-value (:value (test.dom/attrs value)))))))

            (testing "and when :to-model is nil"
              (let [root (render fields/header (dissoc attrs :to-model))
                    form-field (test.dom/query-one root :.form-field)
                    header (test.dom/query-one form-field :.header)
                    value (test.dom/query-one form-field :.value)]
                (testing "passes the target value to :on-change for .header"
                  (spies/reset! on-change)
                  (test.dom/simulate-event header :change [::new-header])
                  (is (spies/called-with? on-change [::new-header ::view-value])))

                (testing "passes the target value to :on-change for .value"
                  (spies/reset! on-change)
                  (test.dom/simulate-event value :change [::new-header])
                  (is (spies/called-with? on-change [::view-key ::new-header])))))))))))

(defn run-tests [] (t/run-tests))
