(ns com.ben-allred.clj-app-simulator.ui.views.main
  (:require [com.ben-allred.clj-app-simulator.templates.views.core :as views]
            [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
            [com.ben-allred.clj-app-simulator.templates.views.forms.file :as file.views]
            [com.ben-allred.clj-app-simulator.templates.views.forms.http :as http.views]
            [com.ben-allred.clj-app-simulator.templates.views.forms.ws :as ws.views]
            [com.ben-allred.clj-app-simulator.ui.views.components.core :as components]
            [com.ben-allred.clj-app-simulator.ui.views.resources :as resources]
            [com.ben-allred.clj-app-simulator.templates.views.simulators :as views.sim]
            [com.ben-allred.clj-app-simulator.utils.simulators :as utils.sims]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.utils.uuids :as uuids]))

(defn root [{:keys [simulators uploads]}]
  [views/root
   [:div.button-row
    [components/menu
     {:items (cond->> [[:http "HTTP Simulator"] [:ws "WS Simulator"]]
               (seq (:data uploads)) (cons [:file "File Server"])
               :always (map (fn [[type label]]
                              {:href  (nav/path-for :new {:query-params {:type type}})
                               :label label})))}
     [:button.button.button-success.pure-button "Create"]]]
   [views.sim/simulators (:data simulators)]])

(defn details [{:keys [page simulators uploads]}]
  (let [id (uuids/->uuid (get-in page [:route-params :id]))
        data (:data simulators)]
    [views/details
     (if-let [{:keys [config] :as simulator} (get data id)]
       (let [[component input] (-> config
                                   (utils.sims/config->section)
                                   (keyword)
                                   (case
                                     :http [http.views/sim]
                                     :ws [ws.views/sim]
                                     :file [file.views/sim uploads]
                                     [views/spinner]))
             detail (assoc simulators :data simulator)]
         (cond-> [components/with-status component detail]
           input (conj input)))
       [:p "This simulator could not be found."])]))

(defn new [state]
  (let [type (get-in state [:page :query-params :type])
        [component input] (case (keyword type)
                            :ws [ws.views/sim-create-form]
                            :http [http.views/sim-create-form]
                            :file [file.views/sim-create-form (:uploads state)]
                            nil)]
    (if component
      [views/new
       state
       (if input
         [components/with-status component input]
         [component])]
      (nav/nav-and-replace! :new {:query-params {:type :http}}))))

(defn resources [{:keys [uploads]}]
  [views/resources
   [components/with-status resources/root uploads]])
