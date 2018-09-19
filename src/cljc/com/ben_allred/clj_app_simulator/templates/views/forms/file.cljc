(ns com.ben-allred.clj-app-simulator.templates.views.forms.file
  (:require #?@(:cljs [[com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]
                       [com.ben-allred.clj-app-simulator.ui.simulators.file.interactions :as interactions]
                       [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as shared.interactions]])
            [com.ben-allred.clj-app-simulator.services.navigation :as nav*]
            [com.ben-allred.clj-app-simulator.templates.fields :as fields]
            [com.ben-allred.clj-app-simulator.templates.resources.file :as resources]
            [com.ben-allred.clj-app-simulator.templates.transformations.file :as tr]
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

(defn file-field [form uploads]
  [fields/select
   (-> {:label "File"}
       (with-attrs form [:response :file]))
   (->> uploads
        (map (juxt :id :filename))
        (sort-by second))])

(defn method-field [form]
  [fields/select
   (-> {:label "HTTP Method"}
       (with-attrs form [:method]))
   resources/file-methods])

(defn sim-edit-form* [id form uploads]
  (let [disabled? #?(:clj true :cljs (or (forms/errors form) (not (forms/changed? form))))]
    [:form.simulator-edit
     #?(:cljs {:on-submit (interactions/update-simulator form id (not disabled?))})
     [name-field form]
     [group-field form]
     [description-field form]
     [status-field form]
     [delay-field form]
     [headers-field form]
     [file-field form uploads]
     [:div.button-row
      [:button.button.button-secondary.pure-button.save-button
       {:disabled disabled?}
       "Save"]
      [:button.button.button-warning.pure-button.reset-button
       {:type :button
        #?@(:clj  [:disabled true]
            :cljs [:on-click (interactions/reset-simulator form id)])}
       "Reset"]]]))

(defn sim-edit-form [{:keys [id] :as sim} _uploads]
  (let [model (tr/sim->model sim)
        form #?(:clj  model
                :cljs (forms/create model resources/validate-existing))]
    (fn [_simulator uploads]
      [sim-edit-form* id form uploads])))

(defn sim [{:keys [config requests id] :as simulator} uploads]
  [:div.simulator
   [views.sim/sim-details simulator]
   [sim-edit-form simulator uploads]
   [:h4 "Requests:"]
   [:ul.requests
    (for [request (sort-by :timestamp > requests)]
      ^{:key (str (:timestamp request))}
      [shared.views/sim-request config request])]
   [:div.button-row
    [:button.button.button-error.pure-button.clear-button
     #?(:clj  {:disabled true}
        :cljs {:disabled (empty? requests)
               :on-click (shared.interactions/clear-requests :http id)})
     "Clear Requests"]
    [:button.button.button-error.pure-button.delete-button
     #?(:clj  {:disabled true}
        :cljs {:on-click (shared.interactions/show-delete-modal id)})
     "Delete Simulator"]]])

(defn sim-create-form* [form uploads]
  (let [disabled? #?(:clj true :cljs (forms/errors form))]
    [:form.simulator-create
     #?(:cljs {:on-submit (interactions/create-simulator form (not disabled?))})
     [method-field form]
     [path-field form]
     [name-field form]
     [group-field form]
     [description-field form]
     [status-field form]
     [delay-field form]
     [headers-field form]
     [file-field form uploads]
     [:div.button-row
      [:button.button.button-secondary.pure-button.save-button
       {:disabled disabled?}
       "Save"]
      [:a.button.button-warning.pure-button.reset-button
       {:href (nav*/path-for :home)}
       "Cancel"]]]))

(defn sim-create-form [uploads]
  (let [model {:path     "/"
               :delay    0
               :method   :file/get
               :response {:status 200
                          :file   (:id (first uploads))}}
        form #?(:clj  model
                :cljs (forms/create model resources/validate-new))]
    (fn [uploads]
      [:div.simulator
       [sim-create-form* form uploads]])))
