(ns com.ben-allred.clj-app-simulator.ui.views.home
    (:require [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]))

(defn root [state]
    [:div
     [:h2 "home"]
     [:div
      [:a {:href (nav/path-for :repos)} "View your projects"]]])
