(ns com.ben-allred.clj-app-simulator.templates.simulators.shared.views
  (:require #?(:cljs [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as interactions])
            [clojure.string :as string]
            [com.ben-allred.clj-app-simulator.services.forms :as forms]
            [com.ben-allred.clj-app-simulator.templates.components.form-fields :as ff]
            [com.ben-allred.clj-app-simulator.utils.datetime :as dt]))

(defn with-attrs [attrs form path model->view view->model]
  (assoc attrs
         :on-change (partial forms/assoc-in form path)
         :value (get-in (forms/current-model form) path)
         :to-view (get-in model->view path)
         :to-model (get-in view->model path)
         :errors (get-in (forms/errors form) path)))

(defn name-field [form model->view view->model]
  [ff/input
   (-> {:label "Name"}
       (with-attrs form [:name] model->view view->model))])

(defn group-field [form model->view view->model]
  [ff/input
   (-> {:label "Group"}
       (with-attrs form [:group] model->view view->model))])

(defn description-field [form model->view view->model]
  [ff/textarea
   (-> {:label "Description"}
       (with-attrs form [:description] model->view view->model))])

(defn path-field [form model->view view->model]
  [ff/input
   (-> {:label "Path"}
       (with-attrs form [:path] model->view view->model))])

(defn sim-details [{{:keys [method path]} :config}]
  [:div.sim-card-identifier
   [:div.sim-card-method (when method (string/upper-case (name method)))]
   [:div.sim-card-path
    [:span.path-prefix "/simulators"]
    [:span.path-user-defined (when (not= "/" path) path)]]])

(defn sim-request [sim {:keys [timestamp] :as request}]
  [:li.request
   {:on-click #?(:clj identity :cljs (interactions/show-request-modal sim request timestamp))}
   [:div
    (dt/format timestamp)]])

