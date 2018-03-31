(ns com.ben-allred.clj-app-simulator.ui.views.components.modal
  (:require [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.ui.utils.dom :as dom]))

(defn ^:private hide-modal []
  (store/dispatch actions/hide-modal))

(defn modal [{:keys [state content title]}]
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
       content]])])
