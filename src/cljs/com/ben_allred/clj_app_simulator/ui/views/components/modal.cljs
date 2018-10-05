(ns com.ben-allred.clj-app-simulator.ui.views.components.modal
  (:require [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.ui.utils.dom :as dom]
            [com.ben-allred.clj-app-simulator.templates.core :as templates]))

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
  (when (not= :unmounted state)
    [:div.modal
     (-> {:on-click hide-modal}
         (templates/classes {"is-active"  (#{:mounted :shown} state)
                             (name state) true}))
     [:div.modal-background]
     [:div.modal-content
      {:on-click dom/stop-propagation}
      [:div.card
       (when title
         [:div.card-header
          [:div.card-header-title title]])
       [:div.card-content
        content]
       (when (seq actions)
         (->> actions
              (map wrap-attrs)
              (map (partial conj [:div.card-footer-item]))
              (into [:div.card-footer])))]]
     [:button.modal-close.is-large
      {:aria-label "close"}]]))
