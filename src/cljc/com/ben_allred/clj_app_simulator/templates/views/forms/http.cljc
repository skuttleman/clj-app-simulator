(ns com.ben-allred.clj-app-simulator.templates.views.forms.http
  (:require
    #?@(:cljs [[com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]
               [com.ben-allred.clj-app-simulator.ui.simulators.http.interactions :as interactions]
               [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as shared.interactions]])
    [com.ben-allred.clj-app-simulator.services.navigation :as nav*]
    [com.ben-allred.clj-app-simulator.templates.transformations.http :as tr]
    [com.ben-allred.clj-app-simulator.templates.fields :as fields]
    [com.ben-allred.clj-app-simulator.templates.resources.http :as resources]
    [com.ben-allred.clj-app-simulator.templates.views.forms.shared :as shared.views]
    [com.ben-allred.clj-app-simulator.templates.views.simulators :as views.sim]
    [com.ben-allred.clj-app-simulator.utils.logging :as log]))

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
   (-> {:label  "Headers"
        :key-fn #(str "header-" (first %))
        :new-fn (constantly ["" ""])
        #?@(:cljs [:change-fn #(apply forms/update-in form [:response :headers] %&)])}
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
  [:form.simulator-edit
   #?(:cljs {:on-submit (interactions/update-simulator form id)})
   [name-field form]
   [group-field form]
   [description-field form]
   [status-field form]
   [delay-field form]
   [headers-field form]
   [body-field form]
   [:div.button-row
    [shared.views/sync-button
     {:form       form
      :text       "Save"
      :sync-text  "Saving"
      :class-name "is-info save-button"
      :disabled   #?(:clj true :cljs (or (forms/display-errors form)
                                         (not (forms/changed? form))))}]
    [shared.views/sync-button
     {:form       form
      :text       "Reset"
      :sync-text  "Resetting"
      :type       :button
      :class-name "is-warning reset-button"
      :disabled   #?(:clj true :cljs false)
      #?@(:cljs [:on-click (interactions/reset-simulator form id)])}]]])

(defn sim-edit-form [{:keys [id] :as sim}]
  (let [model (tr/sim->model sim)
        form #?(:clj  model
                :cljs (forms/create model resources/validate-existing))]
    (fn [_simulator]
      [sim-edit-form* id form])))

(defn sim [{:keys [config requests id] :as simulator}]
  [:div.simulator
   [views.sim/sim-details simulator]
   [sim-edit-form simulator]
   [:h3.title.is-4 "Requests:"]
   [:ul.requests
    (for [request (sort-by :timestamp > requests)]
      ^{:key (str (:timestamp request))}
      [shared.views/sim-request config request])]
   [:div.button-row
    [:button.button.is-danger.clear-button
     {:disabled #?(:clj true :cljs (empty? requests))
      #?@(:cljs [:on-click (shared.interactions/clear-requests :http id)])}
     "Clear Requests"]
    [:button.button.is-danger.delete-button
     #?(:clj  {:disabled true}
        :cljs {:on-click (shared.interactions/show-delete-modal id)})
     "Delete Simulator"]]])

(defn sim-create-form* [form]
  [:div.simulator
   [:form.simulator-create
    #?(:cljs {:on-submit (interactions/create-simulator form)})
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
     [shared.views/sync-button
      {:form       form
       :text       "Save"
       :sync-text  "Saving"
       :disabled   #?(:clj true :cljs (forms/display-errors form))
       :class-name "is-info save-button"}]
     [:a.button.is-warning.reset-button
      {:href (nav*/path-for :home)}
      "Cancel"]]]])

(defn sim-create-form []
  (let [model {:response {:status 200
                          :body   nil}
               :method   :http/get
               :path     "/"
               :delay    0}
        form #?(:clj  model
                :cljs (forms/create model resources/validate-new))]
    (fn []
      [sim-create-form* form])))
