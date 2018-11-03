(ns com.ben-allred.app-simulator.ui.views.components.toast
  (:require
    [com.ben-allred.app-simulator.templates.core :as templates]
    [com.ben-allred.app-simulator.ui.services.store.actions :as actions]
    [com.ben-allred.app-simulator.ui.services.store.core :as store]
    [com.ben-allred.app-simulator.utils.logging :as log]))

(defn toast [messages]
  [:div.toast-container
   [:ul.toast-messages
    (for [[key {:keys [ref level adding? removing?]}] (take 4 (sort-by key messages))]
      [:li.toast-message.message
       (-> {:key key
            :on-click #(store/dispatch (actions/remove-toast key))}
           (templates/classes {"is-success" (= :success level)
                               "is-danger"  (= :error level)
                               "adding"     adding?
                               "removing"   removing?}))
       [:div.message-header]
       [:div.message-body @ref]])]])
