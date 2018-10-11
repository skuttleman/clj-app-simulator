(ns com.ben-allred.clj-app-simulator.templates.views.forms.shared
  (:require #?@(:cljs [[com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]
                       [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as interactions]])
            [com.ben-allred.clj-app-simulator.templates.fields :as fields]
            [com.ben-allred.clj-app-simulator.utils.dates :as dates]))

(defn with-attrs [attrs form path model->view view->model]
  #?(:clj  (assoc attrs :value (get-in form path))
     :cljs (assoc attrs
                  :on-change (partial forms/assoc-in form path)
                  :value (get-in (forms/current-model form) path)
                  :to-view (get-in model->view path)
                  :to-model (get-in view->model path)
                  :errors (get-in (forms/errors form) path))))

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

(defn sim-request [sim {:keys [timestamp] :as request}]
  [:li.request
   #?(:cljs {:on-click (interactions/show-request-modal sim request)})
   [:div
    (dates/format timestamp)]])