(ns com.ben-allred.clj-app-simulator.templates.simulators.file.views
  (:require #?@(:cljs [[com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
                       [com.ben-allred.clj-app-simulator.ui.simulators.file.interactions :as interactions]
                       [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as shared.interactions]])
            [com.ben-allred.clj-app-simulator.services.forms :as forms]
            [com.ben-allred.clj-app-simulator.templates.simulators.shared.views :as shared.views]
            [com.ben-allred.clj-app-simulator.templates.simulators.file.transformations :as tr]
            [com.ben-allred.clj-app-simulator.templates.simulators.file.resources :as resources]
            [com.ben-allred.clj-app-simulator.templates.components.form-fields :as ff]))

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
  [ff/select
   (-> {:label "Status"}
       (with-attrs form [:response :status]))
   resources/statuses])

(defn delay-field [form]
  [ff/input
   (-> {:label "Delay (ms)"}
       (with-attrs form [:delay]))])

(defn headers-field [form]
  [ff/multi
   (-> {:label     "Headers"
        :key-fn    #(str "header-" (first %))
        :new-fn    (constantly ["" ""])
        :change-fn #(apply forms/update-in form [:response :headers] %&)}
       (with-attrs form [:response :headers])
       (dissoc :on-change))
   ff/header])

(defn file-field [form uploads]
  [ff/select
   (-> {:label "File"}
       (with-attrs form [:response :file]))
   (->> uploads
        (map (juxt :id :filename))
        (sort-by second))])

(defn method-field [form]
  [ff/select
   (-> {:label "HTTP Method"}
       (with-attrs form [:method]))
   resources/file-methods])

(defn sim-edit-form* [id form uploads]
  (let [disabled? (or (forms/errors form) (not (forms/changed? form)))]
    [:form.simulator-edit
     {:on-submit #?(:clj identity :cljs (interactions/update-simulator form id (not disabled?)))}
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
       {:type     :button
        :on-click #?(:clj identity :cljs (interactions/reset-simulator form id))}
       "Reset"]]]))

(defn sim-edit-form [{:keys [id] :as sim} _uploads]
  (let [form (-> sim
                 (tr/sim->model)
                 (forms/create resources/validate-existing))]
    (fn [_simulator uploads]
      [sim-edit-form* id form uploads])))

(defn sim [{:keys [config requests id] :as simulator} uploads]
  [:div.simulator
   [shared.views/sim-details simulator]
   [sim-edit-form simulator uploads]
   [:h4 "Requests:"]
   [:ul.requests
    (for [request (sort-by :timestamp > requests)]
      ^{:key (str (:timestamp request))}
      [shared.views/sim-request config request])]
   [:div.button-row
    [:button.button.button-error.pure-button.clear-button
     {:disabled (empty? requests)
      :on-click #?(:clj identity :cljs (shared.interactions/clear-requests id))}
     "Clear Requests"]
    [:button.button.button-error.pure-button.delete-button
     {:on-click #?(:clj identity :cljs (shared.interactions/show-delete-modal id))}
     "Delete Simulator"]]])

(defn sim-create-form* [form uploads]
  (let [disabled? (forms/errors form)]
    [:form.simulator-create
     {:on-submit #?(:clj identity :cljs (interactions/create-simulator form (not disabled?)))}
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
       {:href #?(:clj "#" :cljs (nav/path-for :home))}
       "Cancel"]]]))

(defn sim-create-form [uploads]
  (let [form (-> {:path     "/"
                  :delay    0
                  :method   :file/get
                  :response {:status 200
                             :file   (:id (first uploads))}}
                 (forms/create resources/validate-new))]
    (fn [uploads]
      [:div.simulator
       [sim-create-form* form uploads]])))
