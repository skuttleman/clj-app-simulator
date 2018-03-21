(ns com.ben-allred.clj-app-simulator.ui.views.components.core
    (:refer-clojure :exclude [keyword vector list pr-str set hash-map])
    (:require [goog.string :as gstring]
              [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(defn spinner []
    [:div.loader])

(defn spinner-overlay [show? component]
    (if show?
        [:div
         {:style {:position :relative}}
         [:div.spinner-container
          {:style {:position :absolute :height "50%" :min-height "200px" :min-width "100%"}}
          [spinner]]
         [:div.component-container
          {:style {:position :absolute :height "100%" :min-height "400px" :min-width "100%" :background-color "rgba(0,0,0,0.25)"}}
          ""]
         component]
        component))
