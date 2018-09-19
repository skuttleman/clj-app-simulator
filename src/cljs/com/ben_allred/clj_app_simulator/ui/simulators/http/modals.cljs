(ns com.ben-allred.clj-app-simulator.ui.simulators.http.modals
  (:require [com.ben-allred.clj-app-simulator.templates.fields :as fields]
            [com.ben-allred.clj-app-simulator.templates.views.forms.shared :as shared.views]))

(defn message [form model->view view->model]
  [:div.send-ws-message
   [fields/textarea
    (-> {:label "Message"}
        (shared.views/with-attrs form [:message] model->view view->model))]])
