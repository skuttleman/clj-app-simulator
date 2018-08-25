(ns com.ben-allred.clj-app-simulator.ui.views.main
  (:require [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.ui.views.simulators :as sims]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.views :as http.views]
            [com.ben-allred.clj-app-simulator.ui.simulators.file.views :as file.views]
            [com.ben-allred.clj-app-simulator.ui.simulators.ws.views :as ws.views]
            [com.ben-allred.clj-app-simulator.ui.views.components.core :as components]
            [com.ben-allred.clj-app-simulator.ui.views.resources :as resources]
            [com.ben-allred.clj-app-simulator.ui.utils.simulators :as utils.sims]
            [clojure.string :as string]))

(defn ^:private header-tab [handler page display]
  (let [tag (if (= handler page) :span :a)]
    [tag
     (cond-> {:class-name "tab"}
       (= tag :a) (assoc :href (nav/path-for page)))
     display]))

(defn header [{:keys [handler]}]
  [:header.header
   [:a.home-link
    {:href (nav/path-for :home)}
    [:span.logo]]
   [header-tab handler :home "simulators"]
   [header-tab handler :resources "resources"]])

(defn root [{:keys [simulators uploads home-welcome?]}]
  [:div
   [:h2 "Simulators"]
   [:div.button-row
    [components/menu
     {:items (cond->> [[:http "HTTP Simulator"] [:ws "WS Simulator"]]
               (seq (:data uploads)) (cons [:file "File Server"])
               :always (map (fn [[type label]]
                              {:href  (nav/path-for :new {:query-params {:type type}})
                               :label label})))}
     [:button.button.button-success.pure-button "Create"]]]
   [components/with-status [sims/simulators home-welcome?] simulators]])

(defn details [state]
  (let [id (uuid (get-in state [:page :route-params :id]))
        {:keys [data] :as simulators} (:simulators state)
        {:keys [config] :as simulator} (get data id)
        [component input] (-> config
                              (utils.sims/config->section)
                              (keyword)
                              (case
                                :http [http.views/sim]
                                :ws [ws.views/sim]
                                :file [file.views/sim (:uploads state)]
                                [components/spinner]))
        detail (assoc simulators :data simulator)]
    [:div
     [:h2 "Simulator Details"]
     (cond-> [components/with-status component detail]
       input (conj input))]))

(defn new [state]
  (let [type (get-in state [:page :query-params :type])
        [component input] (case (keyword type)
                            :ws [ws.views/sim-create-form]
                            :http [http.views/sim-create-form]
                            :file [file.views/sim-create-form (:uploads state)]
                            nil)]
    (if component
      [:div
       [:h2 (str "New " (string/upper-case type) " Simulator")]
       (if input
         [components/with-status component input]
         [component])]
      (nav/nav-and-replace! :new {:query-params {:type :http}}))))

(defn resources [state]
  [:div
   [:h2 "Resources"]
   [components/with-status [resources/root (:uploads-welcome? state)] (:uploads state)]])
