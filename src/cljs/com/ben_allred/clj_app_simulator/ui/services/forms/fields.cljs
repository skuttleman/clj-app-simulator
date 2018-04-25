(ns com.ben-allred.clj-app-simulator.ui.services.forms.fields
  (:require [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.utils.fns :as fns]
            [com.ben-allred.clj-app-simulator.ui.utils.dom :as dom]))

(defn ^:private update-by-idx [update idx v]
  (update (fns/=>> (map-indexed #(if (= idx %1) v %2))
                   (vec))))

(defn ^:private remove-by-idx [update idx]
  (update (fns/=>> (map-indexed vector)
                   (remove (comp (partial = idx) first))
                   (mapv second))))

(defn ^:private form-field [{:keys [label errors]} & body]
  (into [:div.form-field
         (when (seq errors)
           {:class-name :errors})
         (when label
           [:label label])]
        body))

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
     attrs
     [:div
      [:input.header
       {:value     k
        :on-change #(on-change (to-model [(dom/target-value %) v]))}]
      [:input.value
       {:value     v
        :on-change #(on-change (to-model [k (dom/target-value %)]))}]]]))

(defn multi [{:keys [key-fn value new-fn change-fn errors] :as attrs} component]
  (let [length (count value)
        last-idx (dec length)]
    [form-field
     (dissoc attrs :errors)
     [:ul.multi
      (for [[idx val :as key] (map-indexed vector value)
            :let [last? (= idx last-idx)]]
        [:li
         {:key (key-fn key)}
         [:div
          [component (-> attrs
                         (dissoc :label)
                         (assoc :value val
                                :on-change (partial update-by-idx change-fn idx)
                                :errors (nth errors idx nil)))]
          [:i.fa.fa-minus
           {:on-click #(remove-by-idx change-fn idx)}]]
         (when last?
           [:i.fa.fa-plus
            {:on-click #(change-fn conj (new-fn length))}])])]]))
