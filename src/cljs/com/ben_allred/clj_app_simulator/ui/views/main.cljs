(ns com.ben-allred.clj-app-simulator.ui.views.main
    (:require [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
              [com.ben-allred.clj-app-simulator.ui.views.auth :as auth]
              [com.ben-allred.clj-app-simulator.ui.utils.core :as utils]))

(defn header [logged-in?]
    [:header.header
     [:div.home-link
      (-> (when logged-in?
              {:on-click #(nav/navigate! :home)})
          (utils/classes {:button logged-in?}))
      [:img.logo {:src "/images/logo.png"}]
      [:h1 "App Simulator"]]
     [:div
      (when logged-in?
          [auth/logout-button])]])
