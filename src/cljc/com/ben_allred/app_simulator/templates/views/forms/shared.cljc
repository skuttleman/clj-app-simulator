(ns com.ben-allred.app-simulator.templates.views.forms.shared
  (:require
    #?@(:cljs [[com.ben-allred.app-simulator.ui.simulators.shared.interactions :as interactions]
               [com.ben-allred.app-simulator.ui.utils.dom :as dom]])
    [clojure.core.async :as async]
    [com.ben-allred.app-simulator.services.forms.core :as forms]
    [com.ben-allred.app-simulator.templates.fields :as fields]
    [com.ben-allred.app-simulator.templates.resources.shared :as shared.resources]
    [com.ben-allred.app-simulator.templates.views.core :as views]
    [com.ben-allred.app-simulator.utils.dates :as dates]
    [com.ben-allred.app-simulator.utils.fns :as fns]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [com.ben-allred.app-simulator.utils.chans :as ch]))

(defn with-attrs [attrs form path model->view view->model]
  (-> attrs
      (assoc :on-change (partial swap! form assoc-in path)
             :value (get-in @form path)
             :to-view (get-in model->view path)
             :to-model (get-in view->model path)
             :errors (get-in (forms/errors form) path)
             :touched? (forms/touched? form path)
             :tried? (forms/tried? form))
      (update :disabled fns/or (forms/syncing? form))))

(defn create-disabled? [form]
  (and (not (forms/valid? form))
       (forms/tried? form)))

(defn edit-disabled? [form]
  (or (create-disabled? form)
      (not (forms/changed? form))))

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

(defn with-sync-action [attrs form on-event]
  (-> attrs
      (update on-event (fn [handler]
                         (fn [event]
                           #?(:cljs (dom/prevent-default event))
                           (when-not (:disabled attrs)
                             (forms/try! form)
                             (when (forms/valid? form)
                               (forms/sync! form)
                               (-> event
                                   (handler)
                                   (ch/peek* (fn [[status value]]
                                               (forms/ready! form status value)))))))))))

(defn sync-button [{:keys [form on-event sync-text text] :as attrs}]
  (let [syncing? (forms/syncing? form)]
    [:button.button.sync-button
     (-> attrs
         (dissoc :form :on-event :sync-text :text)
         (update :disabled fns/or syncing?)
         (cond->
           on-event (with-sync-action form on-event)))
     (if syncing?
       [:div.syncing sync-text [views/spinner]]
       text)]))
