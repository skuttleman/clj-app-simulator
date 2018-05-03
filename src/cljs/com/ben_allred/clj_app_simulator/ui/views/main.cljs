(ns com.ben-allred.clj-app-simulator.ui.views.main
  (:require [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.ui.views.simulators :as sims]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.views :as sim]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.views.components.core :as components]))

(defn header []
  [:header.header
   [:a.home-link {:href (nav/path-for :home)}
    [:img.logo {:src "/images/logo.png"}]
    [:h1 "App Simulator"]]])

(defn root [{{:keys [data status]} :simulators}]
  [:div
   [:h2 "Simulators"]
   [components/with-status status sims/simulators data #(store/dispatch actions/request-simulators)]])

(defn details [state]
  (let [id (uuid (get-in state [:page :route-params :id]))
        {:keys [status data]} (:simulators state)
        simulator (get data id)]
    [:div
     [:h2 "Simulator Details"]
     [components/with-status status sim/sim simulator #(store/dispatch actions/request-simulators)]]))
