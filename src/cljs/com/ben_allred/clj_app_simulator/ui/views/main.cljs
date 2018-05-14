(ns com.ben-allred.clj-app-simulator.ui.views.main
  (:require [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.ui.views.simulators :as sims]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.views :as http.views]
            [com.ben-allred.clj-app-simulator.ui.simulators.ws.views :as ws.views]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.views.components.core :as components]
            [com.ben-allred.clj-app-simulator.ui.utils.simulators :as utils.sims]))

(def ^:private section->component
  {:http http.views/sim
   :ws   ws.views/sim})

(defn header []
  [:header.header
   [:a.home-link {:href (nav/path-for :home)}
    [:img.logo {:src "/images/logo.png"}]
    [:h1 "App Simulator"]]])

(defn root [{{:keys [data status]} :simulators}]
  [:div
   [:h2 "Simulators"]
   [components/with-status status sims/simulators data #(store/dispatch actions/request-simulators)]
   [:div.button-row
    [:a.button.button-success.pure-button
     {:href (nav/path-for :new {:query-params {:type :http}})}
     "Create"]]])

(defn details [state]
  (let [id (uuid (get-in state [:page :route-params :id]))
        {:keys [status data]} (:simulators state)
        {:keys [config] :as simulator} (get data id)
        component (-> config
                      (utils.sims/config->section)
                      (keyword)
                      (section->component))]
    [:div
     [:h2 "Simulator Details"]
     (if component
       [components/with-status status component simulator #(store/dispatch actions/request-simulators)]
       [components/spinner])]))

(defn new [state]
  (let [type (keyword (get-in state [:page :query-params :type]))]
    [:div
     [:h2 "New Simulator"]
     [http.views/sim-create-form]]))
