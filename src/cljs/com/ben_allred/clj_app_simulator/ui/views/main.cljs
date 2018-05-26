(ns com.ben-allred.clj-app-simulator.ui.views.main
  (:require [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.ui.views.simulators :as sims]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.views :as http.views]
            [com.ben-allred.clj-app-simulator.ui.simulators.ws.views :as ws.views]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.views.components.core :as components]
            [com.ben-allred.clj-app-simulator.ui.utils.simulators :as utils.sims]
            [clojure.string :as string]))

(def ^:private section->component
  {:http http.views/sim
   :ws   ws.views/sim})

(defn request-simulators []
  (store/dispatch actions/request-simulators))

(defn header []
  [:header.header
   [:a.home-link {:href (nav/path-for :home)}
    [:img.logo {:src "/images/logo.png"}]
    [:h1 "App Simulator"]]])

(defn root [{{:keys [data status]} :simulators}]
  [:div
   [:h2 "Simulators"]
   [:div.button-row
    [components/menu
     {:items (->> [[:http "HTTP Simulator"] [:ws "WS Simulator"]]
                  (map (fn [[type label]]
                         {:href  (nav/path-for :new {:query-params {:type type}})
                          :label label})))}
     [:button.button.button-success.pure-button "Create"]]]
   [components/with-status status sims/simulators data request-simulators]])

(defn details [state]
  (let [id (uuid (get-in state [:page :route-params :id]))
        {:keys [status data]} (:simulators state)
        {:keys [config] :as simulator} (get data id)
        component (-> config
                      (utils.sims/config->section)
                      (keyword)
                      (section->component components/spinner))]
    [:div
     [:h2 "Simulator Details"]
     [components/with-status status component simulator request-simulators]]))

(defn new [state]
  (let [type (get-in state [:page :query-params :type])
        {:keys [status data]} (:simulators state)
        component (case (keyword type)
                    :ws ws.views/sim-create-form
                    :http http.views/sim-create-form
                    nil)]
    (when component
      [:div
       [:h2 (str "New " (string/upper-case type) " Simulator")]
       [components/with-status status component data request-simulators]])))
