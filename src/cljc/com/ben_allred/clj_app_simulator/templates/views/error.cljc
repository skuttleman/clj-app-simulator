(ns com.ben-allred.clj-app-simulator.templates.views.error
  #?(:cljs (:require [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav])))

(defn not-found [_]
  [:div
   [:h2 "Page not found"]
   [:div
    "Try going "
    [:a.home {:href #?(:clj "#" :cljs (nav/path-for :home))} "home"]]])
