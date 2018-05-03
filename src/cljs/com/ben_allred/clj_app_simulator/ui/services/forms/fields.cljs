(ns com.ben-allred.clj-app-simulator.ui.services.forms.fields
  (:require [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.utils.fns :as fns :include-macros true]
            [com.ben-allred.clj-app-simulator.ui.utils.dom :as dom]))

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
        to-model (or to-model identity)]
    [form-field
     attrs
     [:select
      {:on-change  (comp on-change to-model dom/target-value)
       :value      (to-view value)
       :class-name class-name}
      (for [[option label] options
            :let [option (to-view option)]]
        [:option {:key (str option) :value option} label])]]))

(defn textarea [{:keys [on-change value class-name to-view to-model] :as attrs}]
  (let [to-view (or to-view identity)
        to-model (or to-model identity)]
    [form-field
     attrs
     [:textarea
      {:on-change  (comp on-change to-model dom/target-value)
       :value      (to-view value)
       :class-name class-name}]]))

(defn input [{:keys [on-change value class-name type to-view to-model] :as attrs}]
  (let [to-view (or to-view identity)
        to-model (or to-model identity)]
    [form-field
     attrs
     [:input
      {:on-change  (comp on-change to-model dom/target-value)
       :value      (to-view value)
       :class-name class-name
       :type       (or type :text)}]]))

(defn header [{:keys [value on-change to-view to-model] :as attrs}]
  (let [to-view (or to-view identity)
        to-model (or to-model identity)
        [k v] (to-view value)]
    [form-field
     (update attrs :errors flatten)
     [:div.header-field
      [:input.header-key
       {:value     k
        :on-change #(on-change (to-model [(dom/target-value %) v]))}]
      [:input.header-value
       {:value     v
        :on-change #(on-change (to-model [k (dom/target-value %)]))}]]]))

(defn multi [{:keys [key-fn value new-fn change-fn errors class-name] :as attrs} component]
  [form-field
   (dissoc attrs :errors)
   [:ul.multi
    {:class-name class-name}
    (for [[idx val :as key] (map-indexed vector value)]
      [:li.multi-item
       {:key (key-fn key)}
       [:i.fa.fa-minus.remove-item
        {:on-click #(change-fn (comp vec (remove-by-idx idx)))}]
       [component (-> attrs
                      (dissoc :label)
                      (assoc :value val
                             :errors (nth errors idx nil)
                             :on-change #(change-fn (comp vec (update-by-idx idx %)))))]])]
   [:i.fa.fa-plus.add-item
    {:on-click #(change-fn conj (new-fn (count value)))}]])
