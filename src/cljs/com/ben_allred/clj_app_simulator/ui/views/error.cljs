(ns com.ben-allred.clj-app-simulator.ui.views.error
    (:require [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
              [com.ben-allred.clj-app-simulator.ui.views.main :as main]))

(defn not-found [_]
    [:div
     [main/header]
     [:h2 "Page not found"]
     [:div
      "Try going "
      [:a.home {:href (nav/path-for :home)} "home"]]])
