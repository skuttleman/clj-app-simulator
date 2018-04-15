(ns com.ben-allred.clj-app-simulator.ui.services.forms.fields
  (:require [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(defn ^:private target-value [event]
  (when-let [target (.-target event)]
    (.-value target)))

(defn ^:private form-field [{:keys [label]} & body]
  (into [:div.form-field [:label label]] body))

(defn select [{:keys [on-change value class-name] :as attrs} options]
  [form-field
   attrs
   [:select
    {:on-change (comp on-change target-value)
     :value value
     :class-name class-name}
    (for [[value label] options]
      [:option {:key (str value) :value value} label])]])

(defn textarea [{:keys [on-change value class-name] :as attrs}]
  [form-field
   attrs
   [:textarea
    {:on-change (comp on-change target-value)
     :value value
     :class-name class-name}]])

(defn input [{:keys [on-change value class-name type] :as attrs}]
  [form-field
   attrs
   [:input
    {:on-change (comp on-change target-value)
     :value value
     :class-name class-name
     :type (or type :text)}]])
