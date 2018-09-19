(ns com.ben-allred.clj-app-simulator.ui.views.resources
  (:require [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.modals :as modals]
            [com.ben-allred.clj-app-simulator.ui.views.components.core :as components]
            [com.ben-allred.clj-app-simulator.templates.views.resources :as views.res]))

(defn show-delete-modal [title msg action]
  (fn [_]
    (store/dispatch
      (actions/show-modal
        [modals/confirm-delete msg]
        title
        [:button.button.button-error.pure-button.delete-button
         {:on-click (fn [hide]
                      (fn [_]
                        (hide)
                        (store/dispatch action)))}
         "Delete"]
        [:button.button.button-secondary.pure-button.cancel-button
         "Cancel"]))))

(defn resource [{:keys [id] :as upload}]
  [views.res/resource
   {:on-click (show-delete-modal "Delete Resource" "this resource" (actions/delete-upload id))}
   upload
   [components/upload
    {:on-change  (comp store/dispatch (partial actions/upload-replace id))
     :class-name "button button-warning pure-button"
     :multiple   false}]])

(defn root [uploads]
  [views.res/resources
   {:disabled (empty? uploads)
    :on-click (show-delete-modal "Delete All Resources" "all resources" actions/delete-uploads)}
   [components/upload
    {:on-change  (comp store/dispatch actions/upload)
     :class-name "button button-success pure-button"}]
   resource
   uploads])
