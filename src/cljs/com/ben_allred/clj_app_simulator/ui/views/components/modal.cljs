(ns com.ben-allred.clj-app-simulator.ui.views.components.modal
  (:require [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.utils.dom :as dom]))

(defn ^:private hide-modal []
  (store/dispatch actions/hide-modal))

(defn ^:private wrap-attrs [[tag & args]]
  (let [attrs? (first args)
        [attrs args] (if (map? attrs?)
                       [attrs? (rest args)]
                       [{} args])]
    (into [tag (update attrs :on-click #(if % (% hide-modal) hide-modal))]
          args)))

(defn modal [{:keys [state content title actions]}]
  [:div.modal-wrapper
   {:class-name (name state)
    :on-click   hide-modal}
   (when (not= :unmounted state)
     [:div.modal
      {:on-click dom/stop-propagation}
      [:div.modal-title-bar
       [:i.fa.fa-times.spacer]
       (when title
         [:div.modal-title title])
       [:i.fa.fa-times.button.close-button {:on-click hide-modal}]]
      [:div.modal-content
       content]
      (when (seq actions)
        (->> actions
             (map wrap-attrs)
             (into [:div.modal-actions.button-row])))])])
