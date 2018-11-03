(ns com.ben-allred.app-simulator.templates.fields
  (:require
    #?@(:cljs [[com.ben-allred.app-simulator.ui.utils.dom :as dom]
               [reagent.core :as r]])
    [com.ben-allred.app-simulator.utils.fns :as fns]
    [com.ben-allred.app-simulator.utils.logging :as log]))

(def ^:private empty-value (str ::empty))

(defn ^:private sans-empty [to-model]
  (fn [value]
    (to-model
      (when-not (= value empty-value)
        value))))

(defn ^:private update-by-idx [idx v]
  (map-indexed #(if (= idx %1) v %2)))

(defn ^:private remove-by-idx [idx]
  (comp (map-indexed vector)
        (remove (comp (partial = idx) first))
        (map second)))

(defn ^:private form-field [{:keys [label] :as attrs} & body]
  (let [errors (seq (remove nil? (:errors attrs)))]
    (into [:div.form-field
           (when errors
             {:class-name :errors})
           (when (or label errors)
             [:div.field-info
              (when label
                [:label.label label])
              (when errors
                [:ul.error-list
                 (for [error errors]
                   [:li.error
                    {:key error}
                    error])])])]
          body)))

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
            available? (set (map first options))]
        [form-field
         attrs
         [:select.select
          (-> {:value    (if (available? value)
                           (to-view value)
                           empty-value)
               :disabled #?(:clj true :cljs disabled)
               #?@(:cljs [:on-change (comp on-change (sans-empty to-model) dom/target-value)])}
              (merge (select-keys attrs #{:class-name :ref})))
          (for [[option label attrs] (cond->> options
                                       (not (available? value)) (cons [empty-value "Choose…" {:disabled true}]))
                :let [option (to-view option)]]
            [:option
             (assoc attrs :value option :key (str option))
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
              (merge (select-keys attrs #{:class-name :ref})))
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
              (merge (select-keys attrs #{:class-name :ref})))]]))))

(def input (-input))

(defn header [{:keys [disabled value on-change to-view to-model] :as attrs}]
  (let [to-view (or to-view identity)
        to-model (or to-model identity)
        [k v] (to-view value)]
    [form-field
     (update attrs :errors flatten)
     [:div.header-field
      [:input.input.header-key
       {:value    k
        :disabled #?(:clj true :cljs disabled)
        #?@(:cljs [:on-change #(on-change (to-model [(dom/target-value %) v]))])}]
      [:input.input.header-value
       {:value    v
        :disabled #?(:clj true :cljs disabled)
        #?@(:cljs [:on-change #(on-change (to-model [k (dom/target-value %)]))])}]]]))

(defn multi [{:keys [key-fn value new-fn change-fn errors class-name] :as attrs} component]
  [form-field
   (dissoc attrs :errors)
   [:div
    [:button.button.is-small.add-item
     {:type :button
      #?@(:clj  [:disabled true]
          :cljs [:on-click #(change-fn conj (new-fn (count value)))])}
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
              :cljs [:on-click #(change-fn (fns/intov (remove-by-idx idx)))])}
         [:i.fa.fa-minus.remove-item]]]
       [component (-> attrs
                      (dissoc :label)
                      (assoc :value val
                             :errors (nth errors idx nil)
                             #?@(:cljs [:on-change #(change-fn (fns/intov (update-by-idx idx %)))])))]])]])
