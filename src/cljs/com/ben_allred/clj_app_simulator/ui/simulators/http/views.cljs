(ns com.ben-allred.clj-app-simulator.ui.simulators.http.views
  (:require [clojure.string :as string]
            [com.ben-allred.clj-app-simulator.ui.services.forms.fields :as fields]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.resources :as resources]
            [com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.interactions :as interactions]
            [com.ben-allred.clj-app-simulator.ui.utils.moment :as mo]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.transformations :as tr]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]))

(defn ^:private with-attrs [attrs form path]
  (assoc attrs
    :on-change (partial forms/assoc-in form path)
    :value (get-in (forms/current-model form) path)
    :to-view (get-in tr/model->view path)
    :to-model (get-in tr/view->model path)
    :errors (get-in (forms/errors form) path)))

(defn sim-details [{{:keys [method path]} :config}]
  [:div.sim-card-identifier
   [:div.sim-card-method (when method (string/upper-case (name method)))]
   [:div.sim-card-path path]])

(defn name-field [form]
  [fields/input
   (-> {:label "Name"}
       (with-attrs form [:name]))])

(defn group-field [form]
  [fields/input
   (-> {:label "Group"}
       (with-attrs form [:group]))])

(defn description-field [form]
  [fields/textarea
   (-> {:label "Description"}
       (with-attrs form [:description]))])

(defn status-field [form]
  [fields/select
   (-> {:label "Status"}
       (with-attrs form [:response :status]))
   resources/statuses])

(defn delay-field [form]
  [fields/input
   (-> {:label "Delay (ms)"}
       (with-attrs form [:delay]))])

(defn headers-field [form]
  [fields/multi
   (-> {:label     "Headers"
        :key-fn    #(str "header-" (first %))
        :new-fn    (constantly ["" ""])
        :change-fn #(apply forms/update-in form [:response :headers] %&)}
       (with-attrs form [:response :headers])
       (dissoc :on-change))
   fields/header])

(defn body-field [form]
  [fields/textarea
   (-> {:label "Body"}
       (with-attrs form [:response :body]))])

(defn method-field [form]
  [fields/select
   (-> {:label "HTTP Method"}
       (with-attrs form [:method]))
   resources/http-methods])

(defn path-field [form]
  [fields/input
   (-> {:label "Path"}
       (with-attrs form [:path]))])

(defn sim-edit-form* [id form]
  (let [disabled? (or (forms/errors form) (not (forms/changed? form)))]
    [:form.simulator-edit
     {:on-submit (interactions/update-simulator form id (not disabled?))}
     [name-field form]
     [group-field form]
     [description-field form]
     [status-field form]
     [delay-field form]
     [headers-field form]
     [body-field form]
     [:div.button-row
      [:button.button.button-warning.pure-button.reset-button
       {:type     :button
        :on-click (interactions/reset-simulator form id)}
       "Reset"]
      [:button.button.button-secondary.pure-button.save-button
       {:disabled disabled?}
       "Save"]]]))

(defn sim-edit-form [{:keys [id] :as sim}]
  (let [form (-> sim
                 (tr/sim->model)
                 (forms/create resources/validate-existing))]
    (fn [_simulator]
      [sim-edit-form* id form])))

(defn sim-request [sim {:keys [timestamp] :as request}]
  (let [dt (mo/->moment timestamp)]
    [:li.request
     {:on-click (interactions/show-request-modal sim request dt)}
     [:div
      (mo/from-now dt)]]))

(defn sim [{:keys [config requests id] :as simulator}]
  [:div.simulator
   [:h3 "Simulator"]
   [sim-details simulator]
   [sim-edit-form simulator]
   [:h4 "Requests:"]
   [:ul.requests
    (for [request (sort-by :timestamp > requests)]
      ^{:key (str (:timestamp request))}
      [sim-request config request])]
   [:div.button-row
    [:button.button.button-error.pure-button.clear-button
     {:disabled (empty? requests)
      :on-click (interactions/clear-requests id)}
     "Clear Requests"]
    [:button.button.button-error.pure-button.delete-button
     {:on-click (interactions/show-delete-modal id)}
     "Delete Simulator"]]])

(defn sim-create-form* [form]
  (let [disabled? (forms/errors form)]
    [:form.simulator-create
     {:on-submit (interactions/create-simulator form (not disabled?))}
     [method-field form]
     [path-field form]
     [name-field form]
     [group-field form]
     [description-field form]
     [status-field form]
     [delay-field form]
     [headers-field form]
     [body-field form]
     [:div.button-row
      [:a.button.button-warning.pure-button.reset-button
       {:href (nav/path-for :home)}
       "Cancel"]
      [:button.button.button-secondary.pure-button.save-button
       {:disabled disabled?}

       "Save"]]]))

(defn sim-create-form []
  (let [form (-> {:response {:status 200}
                  :method   :http/get
                  :path     "/"
                  :delay    0}
                 (forms/create resources/validate-new))]
    (fn []
      [:div.simulator
       [sim-create-form* form]])))
