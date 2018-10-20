(ns com.ben-allred.clj-app-simulator.ui.views.components.toast
  (:require
    [com.ben-allred.clj-app-simulator.templates.core :as templates]
    [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
    [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
    [com.ben-allred.clj-app-simulator.utils.logging :as log]
    [com.ben-allred.clj-app-simulator.utils.strings :as strings]))

(defn toast [messages]
  [:div.toast-container
   [:ul.toast-messages
    (for [[key {:keys [ref level adding?]}] (take 2 (sort-by key messages))]
      [:li.toast-message.message
       (-> {:key key}
           (templates/classes {"is-success" (= :success level)
                               "is-danger"  (= :error level)
                               "adding"     adding?}))
       [:div.message-header
        (strings/titlize (name level))
        [:button.delete
         {:aria-label "delete"
          :on-click   #(store/dispatch (actions/remove-toast key))}]]
       [:div.message-body @ref]])]])
