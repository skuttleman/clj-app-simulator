(ns com.ben-allred.clj-app-simulator.ui.views.main
    (:require [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
              [com.ben-allred.clj-app-simulator.ui.utils.core :as utils]))

(defn header []
    [:header.header
     [:a.home-link {:href (nav/path-for :home)}
      [:img.logo {:src "/images/logo.png"}]
      [:h1 "App Simulator"]]])