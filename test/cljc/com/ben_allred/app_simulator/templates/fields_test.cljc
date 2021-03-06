(ns com.ben-allred.app-simulator.templates.fields-test
  (:require
    #?@(:cljs [[com.ben-allred.app-simulator.ui.utils.dom :as dom]
               [reagent.core :as r]])
    [clojure.test :as t :refer [deftest is testing]]
    [com.ben-allred.app-simulator.templates.fields :as fields]
    [test.utils.dom :as test.dom]
    [test.utils.spies :as spies]))

(defn ^:private render [component & args]
  (let [[ff & more] (apply component args)]
    (apply ff more)))

(deftest ^:unit with-auto-focus-test
  (testing "(with-auto-focus)"
    #?(:clj
       (testing "renders without auto-focus? flag"
         (let [component (fields/with-auto-focus ::component)]
           (is (= (component {:some :stuff :auto-focus? ::auto-focus})
                  [::component {:some :stuff}]))))
       :cljs
       (with-redefs [r/create-class (spies/create :reagent-render)
                     dom/focus (spies/create)]
         (spies/reset! r/create-class dom/focus)
         (let [component ((fields/with-auto-focus ::component) {:auto-focus? true})
               {:keys [component-did-mount]} (ffirst (spies/calls r/create-class))]
           (testing "creates a component"
             (is (spies/called-with? r/create-class (spies/matcher map?))))

           (testing "renders the component with attrs"
             (is (= [::component {:some ::attrs} ::child-1 ::child-2]
                    (-> {:some ::attrs :auto-focus? ::auto-focus}
                        (component ::child-1 ::child-2)
                        (update 1 dissoc :ref)))))

           (testing "focuses on mount"
             (let [ref (-> {}
                           (component)
                           (test.dom/attrs)
                           (:ref))]
               (ref ::node)
               (component-did-mount ::this)
               (is (spies/called-with? dom/focus ::node)))))

         (testing "when auto-focus? is false"
           (spies/reset! r/create-class dom/focus)
           (let [component ((fields/with-auto-focus ::component) {:auto-focus? false})
                 {:keys [component-did-mount]} (ffirst (spies/calls r/create-class))]
             (testing "does not focus on mount"
               (is (-> {}
                       (component)
                       (test.dom/attrs)
                       (:ref)
                       (nil?)))
               (component-did-mount ::this)
               (is (spies/never-called? dom/focus)))))))))

(deftest ^:unit select-test
  (testing "(select)"
    (with-redefs [#?@(:cljs [dom/target-value (spies/create first)])
                  fields/with-auto-focus identity]
      (testing "when rendering a select component"
        (let [options [[{:a 1} "A"]
                       [{:b 2} "B"]
                       [{:c 3} "C"]]
              to-view (spies/create #(assoc % :to-view true))
              to-model (spies/create #(assoc % :to-model true))
              on-change (spies/create)
              attrs {:to-view    to-view
                     :to-model   to-model
                     :on-change  on-change
                     :value      {:a 1}
                     :class-name ::class-name
                     :ref        ::ref
                     :label      ::label
                     :errors     {:has :errors}
                     :touched?   true}
              root (render (fields/-select) attrs options)
              form-field (test.dom/query-one root :.form-field)]
          (testing "renders a form-field"
            (is (test.dom/query-one form-field :.form-field.errors)))

          (testing "renders a :label element"
            (is (-> form-field
                    (test.dom/query-one :label)
                    (test.dom/contains? ::label))))

          (let [select (test.dom/query-one form-field :select)
                attrs (test.dom/attrs select)]
            (testing "renders a :select element"
              (is (spies/called-with? to-view {:a 1}))
              (is (= {:a 1 :to-view true} (:value attrs)))
              (is (= ::class-name (:class-name attrs)))
              (is (= ::ref (:ref attrs))))

            #?(:clj
               (testing "is disabled"
                 (is (:disabled attrs)))
               :cljs
               (testing "handles :on-change event"
                 (spies/reset! on-change to-model dom/target-value)
                 (test.dom/simulate-event select :change [{:new :value}])
                 (is (spies/called-with? dom/target-value [{:new :value}]))
                 (is (spies/called-with? to-model {:new :value}))
                 (is (spies/called-with? on-change {:new :value :to-model true})))))

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

          (testing "and when rendering without errors"
            (let [root (render (fields/-select) (dissoc attrs :errors) options)
                  form-field (test.dom/query-one root :.form-field)]
              (testing "does not have an :errors class"
                (is (not (test.dom/query-one form-field :.form-field.errors))))))

          (testing "and when :to-view is nil"
            (let [root (render (fields/-select) (dissoc attrs :to-view) options)
                  form-field (test.dom/query-one root :.form-field)
                  select (test.dom/query-one form-field :select)]
              (testing "has the unchanged :value"
                (is (= {:a 1} (:value (test.dom/attrs select)))))))

          #?(:cljs
             (testing "and when :to-model is nil"
               (let [root (render (fields/-select) (dissoc attrs :to-model) options)
                     form-field (test.dom/query-one root :.form-field)
                     select (test.dom/query-one form-field :select)]
                 (testing "passes the target value to :on-change"
                   (spies/reset! on-change)
                   (test.dom/simulate-event select :change [{:new :value}])
                   (is (spies/called-with? on-change {:new :value})))))))))))

(deftest ^:unit textarea-test
  (testing "(textarea)"
    (with-redefs [#?@(:cljs [dom/target-value (spies/create first)])
                  fields/with-auto-focus identity]
      (testing "when rendering a textarea component"
        (let [to-view (spies/create #(assoc % :to-view true))
              to-model (spies/create #(assoc % :to-model true))
              on-change (spies/create)
              attrs {:to-view    to-view
                     :to-model   to-model
                     :on-change  on-change
                     :value      {:some ::value}
                     :class-name ::class-name
                     :ref        ::ref
                     :label      ::label
                     :errors     {:has :errors}
                     :touched?   true}
              root (render (fields/-textarea) attrs)
              form-field (test.dom/query-one root :.form-field)]
          (testing "renders a form-field"
            (is (test.dom/query-one form-field :.form-field.errors)))

          (testing "renders a :label element"
            (is (-> form-field
                    (test.dom/query-one :label)
                    (test.dom/contains? ::label))))

          (let [textarea (test.dom/query-one form-field :textarea)
                attrs (test.dom/attrs textarea)]
            (testing "renders a :textarea element"
              (is (spies/called-with? to-view {:some ::value}))
              (is (= {:some ::value :to-view true} (:value attrs)))
              (is (= ::class-name (:class-name attrs)))
              (is (= ::ref (:ref attrs))))

            #?(:clj
               (testing "is disabled"
                 (is (:disabled attrs)))
               :cljs
               (testing "handles :on-change event"
                 (spies/reset! on-change to-model dom/target-value)
                 (test.dom/simulate-event textarea :change [{:new :value}])
                 (is (spies/called-with? dom/target-value [{:new :value}]))
                 (is (spies/called-with? to-model {:new :value}))
                 (is (spies/called-with? on-change {:new :value :to-model true})))))

          (testing "and when rendering without errors"
            (let [root (render (fields/-textarea) (dissoc attrs :errors))
                  form-field (test.dom/query-one root :.form-field)]
              (testing "does not have an :errors class"
                (is (not (test.dom/query-one form-field :.form-field.errors))))))

          (testing "and when :to-view is nil"
            (let [root (render (fields/-textarea) (dissoc attrs :to-view))
                  form-field (test.dom/query-one root :.form-field)
                  textarea (test.dom/query-one form-field :textarea)]
              (testing "has the unchanged :value"
                (is (= {:some ::value} (:value (test.dom/attrs textarea)))))))

          #?(:cljs
             (testing "and when :to-model is nil"
               (let [root (render (fields/-textarea) (dissoc attrs :to-model))
                     form-field (test.dom/query-one root :.form-field)
                     textarea (test.dom/query-one form-field :textarea)]
                 (testing "passes the target value to :on-change"
                   (spies/reset! on-change)
                   (test.dom/simulate-event textarea :change [{:new :value}])
                   (is (spies/called-with? on-change {:new :value})))))))))))

(deftest ^:unit input-test
  (testing "(input)"
    (with-redefs [#?@(:cljs [dom/target-value (spies/create first)])
                  fields/with-auto-focus identity]
      (testing "when rendering a input component"
        (let [to-view (spies/create #(assoc % :to-view true))
              to-model (spies/create #(assoc % :to-model true))
              on-change (spies/create)
              attrs {:to-view    to-view
                     :to-model   to-model
                     :on-change  on-change
                     :value      {:some ::value}
                     :class-name ::class-name
                     :ref        ::ref
                     :label      ::label
                     :errors     {:has :errors}
                     :touched?   true
                     :type       ::type}
              root (render (fields/-input) attrs)
              form-field (test.dom/query-one root :.form-field)]
          (testing "renders a form-field"
            (is (test.dom/query-one form-field :.form-field.errors)))

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
              (is (= ::ref (:ref attrs)))
              (is (= ::type (:type attrs))))

            #?(:clj
               (testing "is disabled"
                 (is (:disabled attrs)))
               :cljs
               (testing "handles :on-change event"
                 (spies/reset! on-change to-model dom/target-value)
                 (test.dom/simulate-event input :change [{:new :value}])
                 (is (spies/called-with? dom/target-value [{:new :value}]))
                 (is (spies/called-with? to-model {:new :value}))
                 (is (spies/called-with? on-change {:new :value :to-model true})))))

          (testing "and when rendering without errors"
            (let [root (render (fields/-input) (dissoc attrs :errors))
                  form-field (test.dom/query-one root :.form-field)]
              (testing "does not have an :errors class"
                (is (not (test.dom/query-one form-field :.form-field.errors))))))

          (testing "and when :type is nil"
            (let [root (render (fields/-input) (dissoc attrs :type))
                  form-field (test.dom/query-one root :.form-field)
                  input (test.dom/query-one form-field :input)]
              (testing "has a :type of :text"
                (is (= :text (:type (test.dom/attrs input)))))))

          (testing "and when :to-view is nil"
            (let [root (render (fields/-input) (dissoc attrs :to-view))
                  form-field (test.dom/query-one root :.form-field)
                  input (test.dom/query-one form-field :input)]
              (testing "has the unchanged :value"
                (is (= {:some ::value} (:value (test.dom/attrs input)))))))

          #?(:cljs
             (testing "and when :to-model is nil"
               (let [root (render (fields/-input) (dissoc attrs :to-model))
                     form-field (test.dom/query-one root :.form-field)
                     input (test.dom/query-one form-field :input)]
                 (testing "passes the target value to :on-change"
                   (spies/reset! on-change)
                   (test.dom/simulate-event input :change [{:new :value}])
                   (is (spies/called-with? on-change {:new :value})))))))))))

(deftest ^:unit header-test
  (testing "(header)"
    (with-redefs [#?@(:cljs [dom/target-value (spies/create first)])]
      (testing "when rendering a header component"
        (let [to-view (spies/constantly [::view-key ::view-value])
              to-model (spies/constantly ::model)
              on-change (spies/create)
              attrs {:to-view   to-view
                     :to-model  to-model
                     :on-change on-change
                     :value     [::model-key ::model-value]
                     :label     ::label
                     :errors    [:has :errors]
                     :touched?  true}
              root (render fields/header attrs)
              form-field (test.dom/query-one root :.form-field)]
          (testing "renders a form-field"
            (is (test.dom/query-one form-field :.form-field.errors)))

          (testing "renders a :label element"
            (is (-> form-field
                    (test.dom/query-one :label)
                    (test.dom/contains? ::label))))

          (let [header (test.dom/query-one form-field :input.header-key)
                value (test.dom/query-one form-field :input.header-value)
                header-attrs (test.dom/attrs header)
                value-attrs (test.dom/attrs value)]
            (testing "renders a :header element"
              (is (spies/called-with? to-view [::model-key ::model-value]))
              (is (= ::view-key (:value header-attrs)))
              (is (= ::view-value (:value value-attrs))))

            #?(:clj
               (testing "has a disabled .header-key"
                 (is (:disabled header-attrs)))
               :cljs
               (testing "handles :on-change event for .header-key"
                 (spies/reset! on-change to-model dom/target-value)
                 (test.dom/simulate-event header :change [::new-header])
                 (is (spies/called-with? dom/target-value [::new-header]))
                 (is (spies/called-with? to-model [::new-header ::view-value]))
                 (is (spies/called-with? on-change ::model))))

            #?(:clj
               (testing "has a disabled .header-value"
                 (is (:disabled value-attrs)))
               :cljs
               (testing "handles :on-change event for .header-value"
                 (spies/reset! on-change to-model dom/target-value)
                 (test.dom/simulate-event value :change [::new-header])
                 (is (spies/called-with? dom/target-value [::new-header]))
                 (is (spies/called-with? to-model [::view-key ::new-header]))
                 (is (spies/called-with? on-change ::model)))))

          (testing "and when rendering without errors"
            (let [root (render fields/header (dissoc attrs :errors))
                  form-field (test.dom/query-one root :.form-field)]
              (testing "does not have an :errors class"
                (is (not (test.dom/query-one form-field :.form-field.errors))))))

          (testing "and when :to-view is nil"
            (let [root (render fields/header (dissoc attrs :to-view))
                  form-field (test.dom/query-one root :.form-field)
                  header (test.dom/query-one form-field :.header-key)
                  value (test.dom/query-one form-field :.header-value)]
              (testing "has the unchanged :value"
                (is (= ::model-key (:value (test.dom/attrs header))))
                (is (= ::model-value (:value (test.dom/attrs value)))))))

          #?(:cljs
             (testing "and when :to-model is nil"
               (let [root (render fields/header (dissoc attrs :to-model))
                     form-field (test.dom/query-one root :.form-field)
                     header (test.dom/query-one form-field :.header-key)
                     value (test.dom/query-one form-field :.header-value)]
                 (testing "passes the target value to :on-change for .header-key"
                   (spies/reset! on-change)
                   (test.dom/simulate-event header :change [::new-header])
                   (is (spies/called-with? on-change [::new-header ::view-value])))

                 (testing "passes the target value to :on-change for .header-value"
                   (spies/reset! on-change)
                   (test.dom/simulate-event value :change [::new-header])
                   (is (spies/called-with? on-change [::view-key ::new-header])))))))))))

(deftest ^:unit multi-test
  (testing "(multi)"
    (let [key-spy (spies/create identity)
          new-spy (spies/constantly ::new-value)
          change-spy (spies/create)
          errors [::error-1 ::error-2 ::error-3]
          values [::value-1 ::value-2 ::value-3]
          attrs {:label      ::label
                 :errors     errors
                 :key-fn     key-spy
                 :new-fn     new-spy
                 :on-change  change-spy
                 :class-name ::class-name
                 :value      values}
          root (render fields/multi attrs :component)
          form-field (test.dom/query-one root :.form-field)]
      (testing "renders form-field without errors"
        (is (nil? (:errors (test.dom/attrs form-field)))))

      (testing "when rendering a .multi element"
        (let [multi (test.dom/query-one form-field :.multi)
              items (test.dom/query-all multi :.multi-item)]
          (testing "has a :class-name"
            (is (= ::class-name (:class-name (test.dom/attrs multi))))
            (is (= 3 (count items))))

          (doseq [[idx item] (map-indexed vector items)
                  :let [error (nth errors idx)
                        value (nth values idx)]]
            (testing (str "and when rending multi item " idx)
              (testing "has a key"
                (is (spies/called-with? key-spy [idx value]))
                (is (= [idx value] (:key (test.dom/attrs item)))))

              #?(:cljs
                 (testing "and when rending a remove-button"
                   (let [button (test.dom/query-one item :.remove-item)]
                     (testing "handles :on-click"
                       (spies/reset! change-spy)
                       (test.dom/simulate-event button :click)

                       (let [result (ffirst (spies/calls change-spy))]
                         (is (vector? result))
                         (is (->> values
                                  (map-indexed vector)
                                  (remove (comp #{idx} first))
                                  (map second)
                                  (= result))))))))

              (testing "and when rendering the component"
                (let [component (test.dom/query-one item :component)
                      attrs (test.dom/attrs component)]
                  (testing "does not have a label"
                    (is (nil? (:label attrs))))

                  (testing "has a value"
                    (is (= value (:value attrs))))

                  (testing "has errors"
                    (is (= error (:errors attrs))))

                  #?(:cljs
                     (testing "handles :on-change"
                       (spies/reset! change-spy)
                       (test.dom/simulate-event component :change ::new-value)

                       (let [result (ffirst (spies/calls change-spy))]
                         (is (vector? result))
                         (is (= result (assoc values idx ::new-value))))))))))

          #?(:cljs
             (testing "and when rendering an add-button"
               (let [button (test.dom/query-one form-field :.add-item)]
                 (testing "handles :on-click"
                   (spies/reset! change-spy)
                   (test.dom/simulate-event button :click)

                   (let [result (ffirst (spies/calls change-spy))]
                     (is (spies/called-with? new-spy 3))
                     (is (vector? result))
                     (is (= result (conj values ::new-value)))))))))))))

(defn run-tests []
  (t/run-tests))
