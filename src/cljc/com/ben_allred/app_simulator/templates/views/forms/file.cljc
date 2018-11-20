(ns com.ben-allred.app-simulator.templates.views.forms.file
  (:require
    #?@(:cljs [[com.ben-allred.app-simulator.ui.services.forms.standard :as form.std]
               [com.ben-allred.app-simulator.ui.simulators.file.interactions :as interactions]
               [com.ben-allred.app-simulator.ui.simulators.shared.interactions :as shared.interactions]])
    [com.ben-allred.app-simulator.services.forms.noop :as form.no]
    [com.ben-allred.app-simulator.services.navigation :as nav*]
    [com.ben-allred.app-simulator.templates.fields :as fields]
    [com.ben-allred.app-simulator.templates.resources.file :as resources]
    [com.ben-allred.app-simulator.templates.transformations.file :as tr]
    [com.ben-allred.app-simulator.templates.views.forms.shared :as shared.views]
    [com.ben-allred.app-simulator.templates.views.simulators :as views.sim]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [com.ben-allred.app-simulator.services.forms.core :as forms]))

(defn ^:private with-attrs [attrs form path]
  (shared.views/with-attrs attrs form path tr/model->view tr/view->model))

(defn path-field [form]
  [shared.views/path-field form tr/model->view tr/view->model])

(defn group-field [form]
  [shared.views/group-field form tr/model->view tr/view->model])

(defn description-field [form]
  [shared.views/description-field form tr/model->view tr/view->model])

(defn status-field [form]
  [shared.views/status-field form tr/model->view tr/view->model])

(defn delay-field [form]
  [shared.views/delay-field form tr/model->view tr/view->model])

(defn headers-field [form]
  [shared.views/headers-field form tr/model->view tr/view->model])

(defn method-field [form]
  [shared.views/method-field form resources/file-methods tr/model->view tr/view->model])

(defn name-field
  ([form]
   (name-field form false))
  ([form auto-focus?]
   [fields/input
    (-> {:label       "Name"
         :auto-focus? auto-focus?}
        (with-attrs form [:name]))]))

(defn file-field [form resources]
  [fields/select
   (-> {:label "File"}
       (with-attrs form [:response :file]))
   (->> resources
        (map (juxt :id :filename))
        (sort-by second))])

(defn sim-edit-form* [id form resources]
  [:form.simulator-edit
   #?(:cljs (-> {:on-submit (interactions/update-simulator form id)}
                (shared.views/with-sync-action form :on-submit)))
   [name-field form true]
   [group-field form]
   [description-field form]
   [status-field form]
   [delay-field form]
   [headers-field form]
   [file-field form resources]
   [:div.button-row
    [shared.views/sync-button
     {:form       form
      :text       "Save"
      :sync-text  "Saving"
      :class-name "is-info save-button"
      :disabled   #?(:clj true :cljs (shared.views/edit-disabled? form))}]
    [shared.views/sync-button
     {:form       form
      :text       "Reset"
      :sync-text  "Resetting"
      :type       :button
      :class-name "is-warning reset-button"
      :disabled   #?(:clj true :cljs false)
      #?@(:cljs [:on-click (interactions/reset-simulator id)
                 :on-event :on-click])}]]])

(defn sim-edit-form [{:keys [id] :as sim} resources]
  (let [model (tr/source->model (:config sim))
        model' (cond-> model
                 (not (contains? (set (map :id resources)) (get-in model [:response :file])))
                 (update :response dissoc :file))
        form #?(:clj  (form.no/create model')
                :cljs (form.std/create model' resources/validate-existing))]
    (fn [_simulator resources]
      [sim-edit-form* id form resources])))

(defn sim [{:keys [config requests id] :as simulator} resources]
  [:div.simulator
   [views.sim/sim-details simulator]
   [sim-edit-form simulator resources]
   [:h3.title.is-4 "Requests:"]
   [:ul.requests
    (for [request (sort-by :timestamp > requests)]
      ^{:key (str (:timestamp request))}
      [shared.views/sim-request config request])]
   [:div.button-row
    [:button.button.is-danger.clear-button
     #?(:clj  {:disabled true}
        :cljs {:disabled (empty? requests)
               :on-click (shared.interactions/clear-requests :file id)})
     "Clear Requests"]
    [:button.button.is-danger.delete-button
     #?(:clj  {:disabled true}
        :cljs {:on-click (shared.interactions/show-delete-modal id)})
     "Delete Simulator"]]])

(defn sim-create-form* [form resources]
  [:form.simulator-create
   #?(:cljs (-> {:on-submit (interactions/create-simulator form)}
                (shared.views/with-sync-action form :on-submit)))
   [method-field form]
   [path-field form]
   [name-field form]
   [group-field form]
   [description-field form]
   [status-field form]
   [delay-field form]
   [headers-field form]
   [file-field form resources]
   [:div.button-row
    [shared.views/sync-button
     {:form       form
      :text       "Save"
      :sync-text  "Saving"
      :disabled   #?(:clj true :cljs (shared.views/create-disabled? form))
      :class-name "is-info save-button"}]
    [:a.button.is-warning.reset-button
     {:href (nav*/path-for :home)}
     "Cancel"]]])

(defn sim-create-form [_resources]
  (let [model {:path     "/"
               :delay    0
               :method   :file/get
               :response {:status 200}}
        form #?(:clj  (form.no/create model)
                :cljs (form.std/create model resources/validate-new))]
    (fn [resources]
      [:div.simulator
       [sim-create-form* form resources]])))
