(ns com.ben-allred.app-simulator.templates.fields
  (:require
    #?@(:cljs [[com.ben-allred.app-simulator.ui.utils.dom :as dom]
               [reagent.core :as r]])
    [com.ben-allred.app-simulator.templates.core :as templates]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [com.ben-allred.app-simulator.utils.strings :as strings]))

(def ^:private empty-value (str ::empty))

(defn ^:private sans-empty [to-model]
  (fn [value]
    (to-model
      (when-not (= value empty-value)
        value))))

(defn ^:private modify-coll [xform coll]
  (transduce (comp (map-indexed vector) xform) conj coll))

(defn ^:private update-by-idx [idx value coll]
  (modify-coll (map (fn [[idx' value']]
                 (if (= idx idx')
                   value
                   value')))
               coll))

(defn ^:private remove-by-idx [idx coll]
  (modify-coll (comp (remove (comp #{idx} first)) (map second)) coll))

(defn ^:private form-field [{:keys [errors label touched? tried?]} & body]
  (let [errors (seq (remove nil? errors))
        tooltip? (and errors touched? (not tried?))
        inline? (and errors tried?)]
    [:div.form-field
     (templates/classes {:errors (or tooltip? inline?)})
     [:div.field-info
      [:label.label label]
      (when inline?
        [:ul.error-list
         (for [error errors]
           [:li.error
            {:key error}
            error])])]
     (into [:div
            (cond-> {}
              :always (templates/classes {:tooltip              tooltip?
                                          :is-tooltip-danger    tooltip?
                                          :is-tooltip-multiline tooltip?})
              tooltip? (assoc :data-tooltip (strings/commanate errors)))]
           body)]))

(defn with-auto-focus [component]
  #?(:clj  (fn [attrs & args]
             (into [component (dissoc attrs :auto-focus?)] args))
     :cljs (fn [{:keys [auto-focus?]} & _]
             (let [node-atom (atom nil)
                   ref (fn [node] (some->> node (reset! node-atom)))]
               (r/create-class
                 {:component-did-mount
                  (fn [_this]
                    (when-let [node @node-atom]
                      (when (and node auto-focus?)
                        (dom/focus node))))
                  :reagent-render
                  (fn [attrs & args]
                    (into [component (cond-> (dissoc attrs :auto-focus?)
                                       auto-focus? (assoc :ref ref))]
                          args))})))))

(defn ^:testable -select []
  (with-auto-focus
    (fn [{:keys [disabled on-change value to-view to-model] :as attrs} options]
      (let [to-view (or to-view identity)
            to-model (or to-model identity)
            available? (set (map first options))
            value' (if (available? value)
                     (to-view value)
                     empty-value)]
        [form-field
         attrs
         [:select.select
          (-> {:value    value'
               :disabled #?(:clj true :cljs disabled)
               #?@(:cljs [:on-change (comp on-change (sans-empty to-model) dom/target-value)])}
              (merge (select-keys attrs #{:class-name :on-blur :ref})))
          (for [[option label attrs] (cond->> options
                                       (not (available? value)) (cons [empty-value
                                                                       (str "Choose" #?(:clj "..." :cljs "…"))
                                                                       {:disabled true}]))
                :let [option' (to-view option)]]
            [:option
             (assoc attrs :value option' :key (str option') #?@(:clj [:selected (= option' value')]))
             label])]]))))

(def select (-select))

(defn ^:testable -textarea []
  (with-auto-focus
    (fn [{:keys [disabled on-change value to-view to-model] :as attrs}]
      (let [to-view (or to-view identity)
            to-model (or to-model identity)]
        [form-field
         attrs
         [:textarea.textarea
          (-> {:value      (to-view value)
               :disabled   #?(:clj true :cljs disabled)
               #?@(:cljs [:on-change (comp on-change to-model dom/target-value)])}
              (merge (select-keys attrs #{:class-name :on-blur :ref})))
          #?(:clj (to-view value))]]))))

(def textarea (-textarea))

(defn ^:testable -input []
  (with-auto-focus
    (fn [{:keys [disabled on-change value class-name type to-view to-model] :as attrs}]
      (let [to-view (or to-view identity)
            to-model (or to-model identity)]
        [form-field
         attrs
         [:input.input
          (-> {:value      (to-view value)
               :class-name class-name
               :type       (or type :text)
               :disabled   #?(:clj true :cljs disabled)
               #?@(:cljs [:on-change (comp on-change to-model dom/target-value)])}
              (merge (select-keys attrs #{:class-name :on-blur :ref})))]]))))

(def input (-input))

(defn header [{:keys [disabled value on-change to-view to-model] :as attrs}]
  (let [to-view (or to-view identity)
        to-model (or to-model identity)
        [k v] (to-view value)]
    [form-field
     (update attrs :errors flatten)
     [:div.header-field
      [:input.input.header-key
       (-> {:value    k
            :disabled #?(:clj true :cljs disabled)
            #?@(:cljs [:on-change #(on-change (to-model [(dom/target-value %) v]))])}
           (merge (select-keys attrs #{:on-blur})))]
      [:input.input.header-value
       (-> {:value    v
            :disabled #?(:clj true :cljs disabled)
            #?@(:cljs [:on-change #(on-change (to-model [k (dom/target-value %)]))])}
           (merge (select-keys attrs #{:on-blur})))]]]))

(defn multi [{:keys [key-fn value new-fn on-change errors class-name] :as attrs} component]
  [form-field
   (dissoc attrs :errors)
   [:div
    [:button.button.is-small.add-item
     {:type :button
      #?@(:clj  [:disabled true]
          :cljs [:on-click #(on-change (conj value (new-fn (count value))))])}
     [:i.fa.fa-plus]]]
   [:ul.multi
    {:class-name class-name}
    (for [[idx val :as key] (map-indexed vector value)]
      [:li.multi-item
       {:key (key-fn key)}
       [:div
        [:button.button.is-small.remove-item
         {:type :button
          #?@(:clj  [:disabled true]
              :cljs [:on-click #(on-change (remove-by-idx idx value))])}
         [:i.fa.fa-minus.remove-item]]]
       [component (-> attrs
                      (dissoc :label)
                      (assoc :value val
                             :errors (nth errors idx nil)
                             #?@(:cljs [:on-change #(on-change (update-by-idx idx % value))])))]])]])
