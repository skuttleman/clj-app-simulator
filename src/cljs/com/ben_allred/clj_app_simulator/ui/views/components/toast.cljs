(ns com.ben-allred.clj-app-simulator.ui.views.components.toast
    (:require [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
              [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
              [com.ben-allred.clj-app-simulator.ui.utils.core :as utils]
              [com.ben-allred.clj-app-simulator.utils.logging :as log]
              [clojure.string :as string]))

(defn toast [messages]
    [:div.toast-container
     [:ul.toast-messages
      (for [[key {:keys [text level]}] (sort-by key messages)]
          [:li.toast-message
           (-> {:key key}
               (utils/classes {(name level) true}))
           [:div.toast-text text]
           [:i.fa.fa-times.button.remove-button
            {:on-click #(store/dispatch (actions/remove-toast key))}]])]])