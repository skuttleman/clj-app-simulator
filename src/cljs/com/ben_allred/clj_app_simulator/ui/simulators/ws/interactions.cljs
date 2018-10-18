(ns com.ben-allred.clj-app-simulator.ui.simulators.ws.interactions
  (:require [com.ben-allred.clj-app-simulator.templates.transformations.ws :as tr]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as shared.interactions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.modals :as modals]
            [com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]
            [com.ben-allred.clj-app-simulator.templates.resources.ws :as resources]
            [com.ben-allred.clj-app-simulator.templates.views.forms.shared :as shared.views]
            [com.ben-allred.clj-app-simulator.templates.fields :as fields]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]))


(defn update-simulator [form id]
  (shared.interactions/update-simulator form tr/model->source id))

(defn reset-simulator [form id]
  (shared.interactions/reset-simulator form tr/sim->model id))

(defn create-simulator [form]
  (shared.interactions/create-simulator form tr/model->source))

(defn disconnect-all [simulator-id]
  (fn [_]
    (shared.interactions/do-request (store/dispatch (actions/disconnect-all simulator-id))
                                    (shared.interactions/toaster :success "The web sockets have been disconnected")
                                    (shared.interactions/toaster :error "The web sockets could not be disconnected"))))

(defn disconnect [simulator-id socket-id]
  (fn [_]
    (shared.interactions/do-request (store/dispatch (actions/disconnect simulator-id socket-id))
                                    (shared.interactions/toaster :success "The web socket has been disconnected")
                                    (shared.interactions/toaster :error "The web socket could not be disconnected"))))

(defn send-message [form simulator-id socket-id hide]
  (fn [_]
    (forms/verify! form)
    (if (not (forms/errors form))
      (-> simulator-id
          (actions/send-message socket-id (:message (forms/current-model form)))
          (store/dispatch)
          (shared.interactions/do-request (comp hide (shared.interactions/toaster :success "The message has been sent"))
                                          (shared.interactions/toaster :error "The message could not be sent")))
      (store/dispatch (actions/show-toast :error "You must fix errors before proceeding")))))

(defn send-message-button [attrs form]
  [:button.button.is-info
   (assoc attrs :disabled (forms/display-errors form))
   "Send"])

(defn message-editor [form model->view view->model]
  [:div.send-ws-message
   [fields/textarea
    (-> {:label "Message"}
        (shared.views/with-attrs form [:message] model->view view->model))]])

(defn show-send-modal [simulator-id socket-id]
  (fn [_]
    (let [form (forms/create {} resources/socket-message)]
      (store/dispatch
        (actions/show-modal
          [message-editor form nil resources/view->model]
          (str (if socket-id "Send" "Broadcast") " a Message")
          [send-message-button
           {:on-click (fn [hide]
                        (send-message form
                                      simulator-id
                                      socket-id
                                      hide))}
           form]
          [:button.button.cancel-button
           "Cancel"])))))

(defn show-ws-modal [message]
  (fn [_]
    (store/dispatch
      (actions/show-modal
        [modals/socket-modal message]
        "Message Details"))))
