(ns com.ben-allred.clj-app-simulator.api.services.html
  (:require
    [com.ben-allred.clj-app-simulator.api.services.resources.core :as resources]
    [com.ben-allred.clj-app-simulator.api.services.simulators.core :as simulators]
    [com.ben-allred.clj-app-simulator.services.navigation :as nav*]
    [com.ben-allred.clj-app-simulator.services.ui-reducers :as ui-reducers]
    [com.ben-allred.clj-app-simulator.templates.core :as templates]
    [com.ben-allred.clj-app-simulator.templates.views.core :as views]
    [com.ben-allred.clj-app-simulator.templates.views.forms.file :as file.views]
    [com.ben-allred.clj-app-simulator.templates.views.forms.http :as http.views]
    [com.ben-allred.clj-app-simulator.templates.views.forms.ws :as ws.views]
    [com.ben-allred.clj-app-simulator.templates.views.resources :as views.res]
    [com.ben-allred.clj-app-simulator.templates.views.simulators :as views.sim]
    [com.ben-allred.clj-app-simulator.utils.logging :as log]
    [com.ben-allred.clj-app-simulator.utils.simulators :as utils.sims]
    [com.ben-allred.clj-app-simulator.utils.uuids :as uuids]
    [com.ben-allred.collaj.core :as collaj]
    [hiccup.core :as hiccup]))

(defn ^:private hiccup [tree]
  (hiccup/html tree))

(def ^:private path-for
  (constantly "#"))

(defn ^:private home [state]
  [views/root
   [:div.button-row
    [:button.button.create-button
     {:disabled true}
     "Create"]]
   [views.sim/simulators (get-in state [:simulators :data])]
   [views/spinner]])

(defn ^:private new [state]
  (let [type (get-in state [:page :query-params :type])
        [component input] (case (keyword type)
                            :ws [ws.views/sim-create-form]
                            :file [file.views/sim-create-form (:uploads state)]
                            [http.views/sim-create-form])]
    [views/new
     state
     (if input
       [component (:data input)]
       [component])
     [views/spinner]]))

(defn ^:private details [{:keys [page simulators uploads]}]
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
                                     :file [file.views/sim (:data uploads)]
                                     (constantly nil)))]
         (cond-> [component simulator]
           input (conj input)))
       [:p "This simulator could not be found."])
     [views/spinner]]))

(defn ^:private resource [upload]
  [views.res/resource
   {:disabled true}
   upload
   [:button.button.is-warning.file-cta
    {:disabled true}
    [:span.file-icon
     [:i.fa.fa-upload]]]])

(defn ^:private resources [state]
  [views/resources
   [views.res/resources
    {:disabled true}
    [:button.button.is-primary.file-cta
     {:disabled true}
     [:span.file-icon
      [:i.fa.fa-upload]]]
    resource
    (get-in state [:uploads :data])
    [views/spinner]]])

(def ^:private app-attrs
  {:components {:home      home
                :new       new
                :details   details
                :resources resources}
   :toast      (constantly [:div.toast-container])
   :modal      (constantly [:div.modal-wrapper.unmounted])})

(defn ^:private template [content]
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name :viewport :content "width=device-width, initial-scale=1"}]
    [:title "App Simulator"]
    [:link {:rel  "stylesheet"
            :href "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.6.3/css/font-awesome.min.css"
            :type "text/css"}]
    [:link {:rel  "stylesheet"
            :href "https://cdnjs.cloudflare.com/ajax/libs/bulma/0.7.1/css/bulma.min.css"
            :type "text/css"}]
    [:link {:rel  "stylesheet"
            :href "https://cdn.jsdelivr.net/npm/bulma-tooltip@2.0.2/dist/css/bulma-tooltip.min.css"
            :type "text/css"}]
    [:link {:rel  "stylesheet"
            :href "/css/main.css"
            :type "text/css"}]]
   [:body
    [:div#app content]
    [:script {:type "text/javascript" :src "/js/compiled/app.js"}]
    [:script
     {:type "text/javascript"}
     "com.ben_allred.clj_app_simulator.ui.app.mount_BANG_();"]]])

(defn app [state]
  [views/app* app-attrs state])

(defn tree->html [tree]
  (->> tree
       (hiccup)
       (str "<!DOCTYPE html>")))

(defn build-tree [content]
  (-> content
      (templates/render)
      (template)))

(defn hydrate [page env]
  (let [{:keys [dispatch get-state]} (collaj/create-store ui-reducers/root)
        uploads (resources/list-files env)
        [_ {:keys [simulators]}] (simulators/details env)]
    (dispatch [:files.fetch-all/succeed {:uploads uploads}])
    (dispatch [:simulators/clear])
    (doseq [simulator simulators]
      (dispatch [:simulators.fetch-one/succeed {:simulator simulator}]))
    (-> (get-state)
        (assoc :page page)
        (app)
        (build-tree)
        (tree->html))))

(defn render [{:keys [uri query-string]} env]
  (-> uri
      (cond->
        query-string (str "?" query-string))
      (nav*/match-route)
      (hydrate env)))
