(ns com.ben-allred.clj-app-simulator.ui.simulators.shared.views
  (:require [com.ben-allred.clj-app-simulator.ui.services.forms.fields :as fields]
            [com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]
            [clojure.string :as string]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as interactions]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.ui.utils.moment :as mo]))

(defn with-attrs [attrs form path model->view view->model]
  (assoc attrs
    :on-change (partial forms/assoc-in form path)
    :value (get-in (forms/current-model form) path)
    :to-view (get-in model->view path)
    :to-model (get-in view->model path)
    :errors (get-in (forms/errors form) path)))

(defn name-field [form model->view view->model]
  [fields/input
   (-> {:label "Name"}
       (with-attrs form [:name] model->view view->model))])

(defn group-field [form model->view view->model]
  [fields/input
   (-> {:label "Group"}
       (with-attrs form [:group] model->view view->model))])

(defn description-field [form model->view view->model]
  [fields/textarea
   (-> {:label "Description"}
       (with-attrs form [:description] model->view view->model))])

(defn path-field [form model->view view->model]
  [fields/input
   (-> {:label "Path"}
       (with-attrs form [:path] model->view view->model))])

(defn sim-details [{{:keys [method path]} :config}]
  [:div.sim-card-identifier
   [:div.sim-card-method (when method (string/upper-case (name method)))]
   [:div.sim-card-path path]])

(defn sim-request [sim {:keys [timestamp] :as request}]
  (let [dt (mo/->moment timestamp)]
    [:li.request
     {:on-click (interactions/show-request-modal sim request dt)}
     [:div
      (mo/from-now dt)]]))
