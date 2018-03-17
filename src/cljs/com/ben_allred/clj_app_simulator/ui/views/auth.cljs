(ns com.ben-allred.clj-app-simulator.ui.views.auth
    (:require [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]))

(defn login [state]
    [:div
     [:h2 "login"]
     [:div
      [:a {:href (nav/path-for :home)} "go home"]]])

(defn logout-button []
    [:button.pure-button.pure-button-primary
     {:on-click #(nav/go-to! "/auth/logout")}
     "logout"])
