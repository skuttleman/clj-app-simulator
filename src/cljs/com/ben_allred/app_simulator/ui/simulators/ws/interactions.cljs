(ns com.ben-allred.app-simulator.ui.simulators.ws.interactions
  (:require
    [com.ben-allred.app-simulator.templates.resources.ws :as resources]
    [com.ben-allred.app-simulator.templates.transformations.ws :as tr]
    [com.ben-allred.app-simulator.templates.views.forms.shared :as shared.views]
    [com.ben-allred.app-simulator.ui.services.forms.core :as forms]
    [com.ben-allred.app-simulator.ui.services.forms.standard :as form]
    [com.ben-allred.app-simulator.ui.services.store.actions :as actions]
    [com.ben-allred.app-simulator.ui.services.store.core :as store]
    [com.ben-allred.app-simulator.ui.simulators.shared.interactions :as shared.interactions]
    [com.ben-allred.app-simulator.utils.logging :as log]))


(defn update-simulator [form id]
  (shared.interactions/update-simulator form tr/model->source id))

(defn reset-simulator [form id]
  (shared.interactions/reset-config form tr/sim->model id :ws))

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

(defn send-message [form simulator-id socket-id]
  (fn [hide]
    (fn [_]
      (let [current-model @form]
        (forms/verify! form)
        (when-not (forms/errors form)
          (-> simulator-id
              (actions/send-message socket-id (:message current-model))
              (store/dispatch)
              (shared.interactions/do-request (comp hide
                                                    (shared.interactions/resetter reset! form current-model)
                                                    (shared.interactions/toaster :success "The message has been sent"))
                                              (comp (shared.interactions/resetter forms/ready! form)
                                                    (shared.interactions/toaster :error "The message could not be sent")))))))))

(defn show-send-modal [simulator-id socket-id]
  (fn [_]
    (let [form (form/create {} resources/socket-message)]
      (store/dispatch
        (actions/show-modal
          [:modals/message-editor form nil resources/view->model]
          (str (if socket-id "Send" "Broadcast") " a Message")
          [shared.views/sync-button
            {:form form
             :text "Send"
             :sync-text "Sending"
             :on-click (send-message form simulator-id socket-id)
             :class-name "send-button is-info"}]
          [shared.views/sync-button
           {:form form
            :text "Cancel"
            :sync-text "Cancel"
            :class-name "cancel-button"}])))))

(defn show-ws-modal [message]
  (fn [_]
    (store/dispatch
      (actions/show-modal
        [:modals/socket-modal message]
        "Message Details"))))
