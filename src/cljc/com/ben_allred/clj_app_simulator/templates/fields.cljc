(ns com.ben-allred.clj-app-simulator.templates.fields
  (:require #?(:cljs [com.ben-allred.clj-app-simulator.ui.utils.dom :as dom])
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.utils.fns :as fns :include-macros true]))

(def ^:private empty-value (str ::empty))

(defn ^:private sans-empty [to-model]
  (fn [value]
    (to-model
      (when-not (= value empty-value)
        value))))

(defn ^:private update-by-idx [idx v]
  (fns/=>> (map-indexed #(if (= idx %1) v %2))))

(defn ^:private remove-by-idx [idx]
  (fns/=>> (map-indexed vector)
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
                [:label label])
              (when errors
                [:ul.error-list
                 (for [error errors]
                   [:li.error
                    {:key error}
                    error])])])]
          body)))

(defn select [{:keys [on-change value class-name to-view to-model] :as attrs} options]
  (let [to-view (or to-view identity)
        to-model (or to-model identity)
        available? (set (map first options))]
    [form-field
     attrs
     [:select
      {:class-name class-name
       :value      (if (available? value)
                     (to-view value)
                     empty-value)
       #?@(:clj  [:disabled true]
           :cljs [:on-change (comp on-change (sans-empty to-model) dom/target-value)])}
      (for [[option label attrs] (cond->> options
                                   (not (available? value)) (cons [empty-value "Choose…" {:disabled true}]))
            :let [option (to-view option)]]
        [:option
         (assoc attrs :value option :key (str option))
         label])]]))

(defn textarea [{:keys [on-change value class-name to-view to-model] :as attrs}]
  (let [to-view (or to-view identity)
        to-model (or to-model identity)]
    [form-field
     attrs
     [:textarea
      {:value      (to-view value)
       :class-name class-name
       #?@(:clj  [:disabled true]
           :cljs [:on-change (comp on-change to-model dom/target-value)])}]]))

(defn input [{:keys [on-change value class-name type to-view to-model] :as attrs}]
  (let [to-view (or to-view identity)
        to-model (or to-model identity)]
    [form-field
     attrs
     [:input
      {:value      (to-view value)
       :class-name class-name
       :type       (or type :text)
       #?@(:clj  [:disabled true]
           :cljs [:on-change (comp on-change to-model dom/target-value)])}]]))

(defn header [{:keys [value on-change to-view to-model] :as attrs}]
  (let [to-view (or to-view identity)
        to-model (or to-model identity)
        [k v] (to-view value)]
    [form-field
     (update attrs :errors flatten)
     [:div.header-field
      [:input.header-key
       {:value k
        #?@(:clj  [:disabled true]
            :cljs [:on-change #(on-change (to-model [(dom/target-value %) v]))])}]
      [:input.header-value
       {:value v
        #?@(:clj  [:disabled true]
            :cljs [:on-change #(on-change (to-model [k (dom/target-value %)]))])}]]]))

(defn multi [{:keys [key-fn value new-fn change-fn errors class-name] :as attrs} component]
  [form-field
   (dissoc attrs :errors)
   [:ul.multi
    {:class-name class-name}
    (for [[idx val :as key] (map-indexed vector value)]
      [:li.multi-item
       {:key (key-fn key)}
       [:i.fa.fa-minus.remove-item
        #?(:cljs {:on-click #(change-fn (comp vec (remove-by-idx idx)))})]
       [component (-> attrs
                      (dissoc :label)
                      (assoc :value val
                             :errors (nth errors idx nil)
                             #?@(:cljs [:on-change #(change-fn (comp vec (update-by-idx idx %)))])))]])]
   [:i.fa.fa-plus.add-item
    #?(:cljs {:on-click #(change-fn conj (new-fn (count value)))})]])
