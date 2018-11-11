(ns com.ben-allred.app-simulator.templates.views.forms.shared
  (:require
    #?@(:cljs [[com.ben-allred.app-simulator.ui.services.forms.core :as forms]
               [com.ben-allred.app-simulator.ui.simulators.shared.interactions :as interactions]])
    [com.ben-allred.app-simulator.templates.fields :as fields]
    [com.ben-allred.app-simulator.templates.resources.shared :as shared.resources]
    [com.ben-allred.app-simulator.utils.dates :as dates]
    [com.ben-allred.app-simulator.utils.fns :as fns]
    [com.ben-allred.app-simulator.utils.logging :as log]))

(defn with-attrs [attrs form path model->view view->model]
  #?(:clj  (assoc attrs :value (get-in form path))
     :cljs (-> attrs
               (assoc :on-change (partial forms/assoc-in! form path)
                      :value (get-in @form path)
                      :to-view (get-in model->view path)
                      :to-model (get-in view->model path)
                      :errors (get-in (forms/errors form) path)
                      :verified? (forms/verified? form)
                      :touched? (forms/touched? form path))
               (update :disabled fns/or (forms/syncing? form)))))

(defn path-field [form model->view view->model]
  [fields/input
   (-> {:label "Path"}
       (with-attrs form [:path] model->view view->model))])

(defn group-field [form model->view view->model]
  [fields/input
   (-> {:label "Group"}
       (with-attrs form [:group] model->view view->model))])

(defn description-field [form model->view view->model]
  [fields/textarea
   (-> {:label "Description"}
       (with-attrs form [:description] model->view view->model))])

(defn status-field [form model->view view->model]
  [fields/select
   (-> {:label "Status"}
       (with-attrs form [:response :status] model->view view->model))
   shared.resources/statuses])

(defn delay-field [form model->view view->model]
  [fields/input
   (-> {:label "Delay (ms)"
        :type :number}
       (with-attrs form [:delay] model->view view->model))])

(defn headers-field [form model->view view->model]
  [fields/multi
   (-> {:label  "Headers"
        :key-fn #(str "header-" (first %))
        :new-fn (constantly ["" ""])}
       (with-attrs form [:response :headers] model->view view->model))
   fields/header])

(defn method-field [form methods model->view view->model]
  [fields/select
   (-> {:label       "HTTP Method"
        :auto-focus? true}
       (with-attrs form [:method] model->view view->model))
   methods])

(defn sim-request [sim {:keys [timestamp] :as request}]
  [:li.request.info
   #?(:cljs {:on-click (interactions/show-request-modal sim request)})
   [:div
    (dates/format timestamp)]])

(defn sync-button [attrs]
  #?(:clj
     [:button.button.sync-button
      (dissoc attrs :form :text :sync-text)
      (:text attrs)]
     :cljs
     [forms/sync-button attrs]))
