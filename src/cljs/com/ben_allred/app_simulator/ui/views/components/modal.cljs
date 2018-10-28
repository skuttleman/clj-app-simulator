(ns com.ben-allred.app-simulator.ui.views.components.modal
  (:require
    [com.ben-allred.app-simulator.templates.core :as templates]
    [com.ben-allred.app-simulator.ui.services.store.actions :as actions]
    [com.ben-allred.app-simulator.ui.services.store.core :as store]
    [com.ben-allred.app-simulator.ui.utils.dom :as dom]
    [com.ben-allred.app-simulator.utils.colls :as colls]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [com.ben-allred.app-simulator.ui.simulators.shared.modals :as modals]))

(def ^:private id->component
  {:modals/confirm-delete modals/confirm-delete
   :modals/request-modal modals/request-modal
   :modals/message-editor modals/message-editor
   :modals/socket-modal modals/socket-modal})

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
        (update content 0 id->component)]
       (when (seq actions)
         (into [:div.card-footer]
               (comp (map wrap-attrs)
                     (map (colls/onto [:div.card-footer-item])))
               actions))]]
     [:button.modal-close.is-large
      {:aria-label "close"}]]))
