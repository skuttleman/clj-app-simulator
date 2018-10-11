(ns com.ben-allred.clj-app-simulator.ui.simulators.ws.interactions
  (:require [com.ben-allred.clj-app-simulator.templates.transformations.ws :as tr]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as shared.interactions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.modals :as modals]
            [com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]
            [com.ben-allred.formation.core :as f]
            [com.ben-allred.clj-app-simulator.templates.resources.ws :as resources]
            [com.ben-allred.clj-app-simulator.templates.views.forms.shared :as shared.views]
            [com.ben-allred.clj-app-simulator.templates.fields :as fields]))


(defn update-simulator [form id submittable?]
  (shared.interactions/update-simulator form tr/model->source id submittable?))

(defn reset-simulator [form id]
  (shared.interactions/reset-simulator form tr/sim->model id))

(defn create-simulator [form submittable?]
  (shared.interactions/create-simulator form tr/model->source submittable?))

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

(defn send-message [simulator-id socket-id message hide]
  (fn [_]
    (-> simulator-id
        (actions/send-message socket-id message)
        (store/dispatch)
        (shared.interactions/do-request (comp hide (shared.interactions/toaster :success "The message has been sent"))
                                        (shared.interactions/toaster :error "The message could not be sent")))))

(defn send-message-button [attrs form]
  [:button.button.is-info
   (assoc attrs :disabled (forms/errors form))
   "Send"])

(defn message-editor [form model->view view->model]
  [:div.send-ws-message
   [fields/textarea
    (-> {:label "Message"}
        (shared.views/with-attrs form [:message] model->view view->model))]])

(defn show-send-modal [simulator-id socket-id]
  (fn [_]
    (let [form (forms/create {:message ""} resources/socket-message)]
      (store/dispatch
        (actions/show-modal
          [message-editor form nil nil]
          (str (if socket-id "Send" "Broadcast") " a Message")
          [send-message-button
           {:on-click (fn [hide]
                        (send-message simulator-id
                                      socket-id
                                      (:message (forms/current-model form))
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
