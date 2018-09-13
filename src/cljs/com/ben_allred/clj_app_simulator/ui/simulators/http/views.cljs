(ns com.ben-allred.clj-app-simulator.ui.simulators.http.views
  (:require [com.ben-allred.clj-app-simulator.ui.services.forms.fields :as fields]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.resources :as resources]
            [com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.interactions :as interactions]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.transformations :as tr]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.views :as shared.views]
            [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as shared.interactions]))

(defn ^:private with-attrs [attrs form path]
  (shared.views/with-attrs attrs form path tr/model->view tr/view->model))

(defn path-field [form]
  [shared.views/path-field form tr/model->view tr/view->model])

(defn name-field [form]
  [shared.views/name-field form tr/model->view tr/view->model])

(defn group-field [form]
  [shared.views/group-field form tr/model->view tr/view->model])

(defn description-field [form]
  [shared.views/description-field form tr/model->view tr/view->model])

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
      [:button.button.button-secondary.pure-button.save-button
       {:disabled disabled?}
       "Save"]
      [:button.button.button-warning.pure-button.reset-button
       {:type     :button
        :on-click (interactions/reset-simulator form id)}
       "Reset"]]]))

(defn sim-edit-form [{:keys [id] :as sim}]
  (let [form (-> sim
                 (tr/sim->model)
                 (forms/create resources/validate-existing))]
    (fn [_simulator]
      [sim-edit-form* id form])))

(defn sim [{:keys [config requests id] :as simulator}]
  [:div.simulator
   [shared.views/sim-details simulator]
   [sim-edit-form simulator]
   [:h4 "Requests:"]
   [:ul.requests
    (for [request (sort-by :timestamp > requests)]
      ^{:key (str (:timestamp request))}
      [shared.views/sim-request config request])]
   [:div.button-row
    [:button.button.button-error.pure-button.clear-button
     {:disabled (empty? requests)
      :on-click (shared.interactions/clear-requests :http id)}
     "Clear Requests"]
    [:button.button.button-error.pure-button.delete-button
     {:on-click (shared.interactions/show-delete-modal id)}
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
      [:button.button.button-secondary.pure-button.save-button
       {:disabled disabled?}
       "Save"]
      [:a.button.button-warning.pure-button.reset-button
       {:href (nav/path-for :home)}
       "Cancel"]]]))

(defn sim-create-form []
  (let [form (-> {:response {:status 200}
                  :method   :http/get
                  :path     "/"
                  :delay    0}
                 (forms/create resources/validate-new))]
    (fn []
      [:div.simulator
       [sim-create-form* form]])))
