(ns com.ben-allred.clj-app-simulator.ui.views.components.core
  (:require [com.ben-allred.clj-app-simulator.utils.logging :as log]))

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

(defn with-status [_status _component item request]
  (when (empty? item)
    (request))
  (fn [status component item _request]
    (cond
      (and (= :available status) (seq item))
      [component item]

      (= :available status)
      [:div "Not found"]

      :else
      [spinner])))
