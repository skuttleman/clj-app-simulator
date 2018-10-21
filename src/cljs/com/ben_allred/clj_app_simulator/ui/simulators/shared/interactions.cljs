(ns com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions
  (:require
    [cljs.core.async :as async]
    [com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]
    [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
    [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
    [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
    [com.ben-allred.clj-app-simulator.ui.utils.dom :as dom]
    [com.ben-allred.clj-app-simulator.utils.fns :as fns :include-macros true]
    [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(defn toaster [level default-msg]
  (fn [body]
    (let [message (:message body default-msg)]
      (->> message
           (actions/show-toast level)
           (store/dispatch))
      body)))

(defn resetter [f form & args]
  (fn [body]
    (apply f form args)
    body))

(defn do-request
  ([request]
   (do-request request identity))
  ([request on-success]
   (do-request request on-success identity))
  ([request on-success on-failure]
   (async/go
     (let [[status body] (async/<! request)]
       (if (= :success status)
         (on-success body)
         (on-failure body))))))

(defn update-simulator [form model->source id]
  (fn [e]
    (let [current-model (forms/current-model form)]
      (dom/prevent-default e)
      (forms/verify! form)
      (if (and (not (forms/errors form))
               (forms/changed? form))
        (do-request (->> current-model
                         (model->source)
                         (actions/update-simulator id)
                         (store/dispatch))
                    (comp (resetter forms/reset! form current-model)
                          (toaster :success "The simulator has been updated"))
                    (comp (resetter forms/ready! form)
                          (toaster :error "The simulator could not be updated")))
        (store/dispatch (actions/show-toast :error "You must fix errors before proceeding"))))))

(defn clear-requests [type id]
  (fn [_]
    (let [[name action] (case type
                          :http ["requests" :simulators.http/reset-requests]
                          :ws ["messages" :simulators.ws/reset-messages]
                          nil)]
      (do-request (store/dispatch (actions/clear-requests action id))
                  (toaster :success (str "The " name " have been cleared"))
                  (toaster :error (str "The " name " could not be cleared"))))))

(defn delete-sim [id]
  (fn [hide]
    (fn [_]
      (do-request (store/dispatch (actions/delete-simulator id))
                  (comp (fn [_]
                          (when hide
                            (hide))
                          (nav/navigate! :home))
                        (toaster :success "The simulator has been deleted"))
                  (toaster :error "The simulator could not be deleted")))))

(defn reset-simulator [form sim->model id]
  (fn [_]
    (do-request
      (store/dispatch (actions/reset-simulator id))
      (comp (fns/=>> (sim->model) (forms/reset! form))
            (toaster :success "The simulator has been reset"))
      (toaster :error "The simulator could not be reset"))))

(defn create-simulator [form model->source]
  (fn [e]
    (let [current-model (forms/current-model form)]
      (dom/prevent-default e)
      (forms/verify! form)
      (if (not (forms/errors form))
        (do-request (-> current-model
                        (model->source)
                        (actions/create-simulator)
                        (store/dispatch))
                    (comp (fns/=>> (:simulator)
                                   (:id)
                                   (assoc {} :id)
                                   (nav/nav-and-replace! :details))
                          (resetter forms/reset! form current-model)
                          (toaster :success "The simulator has been created"))
                    (comp (resetter forms/ready! form)
                          (toaster :error "The simulator could not be created")))
        (store/dispatch (actions/show-toast :error "You must fix errors before proceeding"))))))

(defn show-delete-modal [id]
  (fn [_]
    (store/dispatch
      (actions/show-modal
        [:modals/confirm-delete "this simulator"]
        "Delete Simulator"
        [:button.button.is-danger.delete-button
         {:on-click (delete-sim id)}
         "Delete"]
        [:button.button.cancel-button
         "Cancel"]))))

(defn show-request-modal [sim request]
  (fn [_]
    (store/dispatch
      (actions/show-modal
        [:modals/request-modal sim request]
        "Request Details"))))
