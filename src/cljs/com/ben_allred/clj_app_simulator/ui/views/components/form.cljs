(ns com.ben-allred.clj-app-simulator.ui.views.components.form
    (:require [reagent.core :as r]
              [com.ben-allred.clj-app-simulator.utils.maps :as maps]
              [com.ben-allred.clj-app-simulator.ui.services.events :as events]
              [com.ben-allred.clj-app-simulator.utils.logging :as log]
              [clojure.string :as string]
              [com.ben-allred.clj-app-simulator.ui.utils.core :as utils]))

(defn ^:private input* [tag {:keys [on-change] :as attrs}]
    [tag
     (-> attrs
         (utils/classes {:input true})
         (update :auto-focus #(if (nil? %) true %))
         (maps/update-maybe :on-change comp #(.-value (.-target %)))
         (maps/update-maybe :on-key-down comp events/->key-code))])

(defn input
    ([initial-value attrs]
     [input :input initial-value attrs])
    ([tag initial-value attrs]
     (let [value (r/atom nil)]
         (r/create-class
             {:component-did-mount
              (fn [] (reset! value initial-value))
              :reagent-render
              (fn [tag initial-value {:keys [on-submit on-cancel on-change]}]
                  [input* tag (cond-> attrs
                                  :always (assoc :type :text
                                                 :value @value
                                                 :on-key-down #(case %
                                                                   :esc (and on-cancel (on-cancel))
                                                                   :enter (and on-submit (on-submit @value))
                                                                   nil))
                                  on-change (update :on-change juxt #(reset! value %))
                                  (not on-change) (assoc :on-change #(reset! value %))
                                  on-submit (assoc :on-blur #(on-submit @value))
                                  :always (dissoc :on-submit :on-cancel))])}))))
