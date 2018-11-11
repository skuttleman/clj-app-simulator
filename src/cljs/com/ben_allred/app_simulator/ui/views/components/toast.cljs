(ns com.ben-allred.app-simulator.ui.views.components.toast
  (:require
    [com.ben-allred.app-simulator.templates.core :as templates]
    [com.ben-allred.app-simulator.ui.services.store.actions :as actions]
    [com.ben-allred.app-simulator.ui.services.store.core :as store]
    [com.ben-allred.app-simulator.utils.logging :as log]))

(defn ^:private toast-message [_key _message]
  (let [height (atom nil)]
    (fn [key {:keys [ref level adding? removing?]}]
      [:li.toast-message.message
       (cond-> {:ref (fn [node]
                       (some->> node
                                (.getBoundingClientRect)
                                (.-height)
                                (reset! height)))
                :style {:margin-top "1px"}}
         :always (templates/classes {"is-success" (= :success level)
                                     "is-danger"  (= :error level)
                                     "adding"     adding?
                                     "removing"   removing?})
         (and removing? @height) (update :style assoc :margin-top (str "-" @height "px")))
       [:div.message-header]
       [:div.message-body
        [:div.body-text @ref]
        [:button.delete
         {:aria-label "delete"
          :on-click   #(store/dispatch (actions/remove-toast key))}]]])))

(defn toast [messages]
  [:div.toast-container
   [:ul.toast-messages
    (for [[key message] (take 4 (sort-by key messages))]
      ^{:key key}
      [toast-message key message])]])
