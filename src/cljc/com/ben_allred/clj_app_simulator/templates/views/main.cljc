(ns com.ben-allred.clj-app-simulator.templates.views.main
  (:require #?@(:cljs [[com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
                       [com.ben-allred.clj-app-simulator.ui.views.components.modal :as modal]
                       [com.ben-allred.clj-app-simulator.ui.views.components.toast :as toast]])
    [clojure.string :as string]
    [com.ben-allred.clj-app-simulator.templates.components.core :as components]
    [com.ben-allred.clj-app-simulator.templates.simulators.file.views :as file.views]
    [com.ben-allred.clj-app-simulator.templates.simulators.http.views :as http.views]
    [com.ben-allred.clj-app-simulator.templates.simulators.ws.views :as ws.views]
    [com.ben-allred.clj-app-simulator.templates.views.error :as error]
    [com.ben-allred.clj-app-simulator.templates.views.resources :as resources]
    [com.ben-allred.clj-app-simulator.templates.views.simulators :as sims]
    [com.ben-allred.clj-app-simulator.utils.logging :as log]
    [com.ben-allred.clj-app-simulator.utils.simulators :as utils.sims]
    [com.ben-allred.clj-app-simulator.utils.uuids :as uuids]))

(defn ^:private header-tab [handler page display]
  (let [tag (if (= handler page) :span :a)]
    [tag
     (cond-> {:class-name "tab"}
       (= tag :a) (assoc :href #?(:clj "#" :cljs (nav/path-for page))))
     display]))

(defn header [{:keys [handler]}]
  [:header.header
   [:a.home-link
    {:href #?(:clj "#" :cljs (nav/path-for :home))}
    [:span.logo]]
   [header-tab handler :home "simulators"]
   [header-tab handler :resources "resources"]])

(defn home [{:keys [simulators uploads home-welcome?]}]
  [:div
   [:h2 "Simulators"]
   [components/with-status [sims/simulators home-welcome? uploads] simulators]])

(defn details [state]
  (let [id (uuids/->uuid (get-in state [:page :route-params :id]))
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
        component (case (keyword type)
                    :ws ws.views/sim-create-form
                    :http http.views/sim-create-form
                    :file file.views/sim-create-form
                    nil)]
    (if component
      [:div
       [:h2 (str "New " (string/upper-case type) " Simulator")]
       [components/with-status component (:uploads state)]]
      #?(:cljs (nav/nav-and-replace! :new {:query-params {:type :http}})))))

(defn resources [state]
  [:div
   [:h2 "Resources"]
   [components/with-status [resources/root (:uploads-welcome? state)] (:uploads state)]])

(def ^:private components
  {:home      home
   :new       new
   :details   details
   :resources resources})

(defn app [{:keys [page] :as state}]
  (let [component (components (:handler page) error/not-found)]
    [:div.app
     #?(:clj  [:div.toast-container]
        :cljs [toast/toast (:toasts state)])
     [:div.scrollable
      [header page]
      [:main.main
       [component state]]]
     #?(:clj  [:div.modal-wrapper]
        :cljs [modal/modal (:modal state)])]))
