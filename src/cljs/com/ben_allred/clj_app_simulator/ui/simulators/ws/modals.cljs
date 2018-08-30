(ns com.ben-allred.clj-app-simulator.ui.simulators.ws.modals
  (:require [com.ben-allred.clj-app-simulator.templates.simulators.shared.views :as shared.views]
            [com.ben-allred.clj-app-simulator.templates.components.form-fields :as ff]))

(defn message [form]
  [:div.send-ws-message
   [ff/textarea
    (-> {:label "Message"}
        (shared.views/with-attrs form [:message] nil nil))]])
