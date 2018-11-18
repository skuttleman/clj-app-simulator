(ns com.ben-allred.app-simulator.ui.simulators.shared.interactions
  (:require
    [com.ben-allred.app-simulator.services.forms.core :as forms]
    [com.ben-allred.app-simulator.ui.services.navigation :as nav]
    [com.ben-allred.app-simulator.ui.services.store.actions :as actions]
    [com.ben-allred.app-simulator.ui.services.store.core :as store]
    [com.ben-allred.app-simulator.utils.chans :as ch :include-macros true]
    [com.ben-allred.app-simulator.utils.logging :as log]))

(defn creatable? [form]
  (forms/valid? form))

(defn updatable? [form]
  (and (creatable? form)
       (forms/changed? form)))

(defn toast [body level default-msg]
  (->> (:message body default-msg)
       (actions/show-toast level)
       (store/dispatch)))

(defn update-simulator [form model->source id]
  (fn [_]
    (if (updatable? form)
      (let [current-model @form]
        (-> current-model
            (model->source)
            (->> (actions/update-simulator id))
            (store/dispatch)
            (ch/peek (fn [body]
                       (reset! form current-model)
                       (toast body :success "The simulator has been updated"))
                     #(toast % :error "The simulator could not be updated"))))
      (ch/reject))))

(defn clear-requests [type id]
  (fn [_]
    (let [name (if (= type :ws)
                 "messages"
                 "requests")]
      (-> (actions/clear-requests id (keyword type :requests))
          (store/dispatch)
          (ch/peek #(toast % :success (str "The " name " have been cleared"))
                   #(toast % :error (str "The " name " could not be cleared")))))))

(defn delete-sim [id]
  (fn [hide]
    (fn [_]
      (-> id
          (actions/delete-simulator)
          (store/dispatch)
          (ch/peek (fn [body]
                     (toast body :success "The simulator has been deleted")
                     (nav/navigate! :home))
                   #(toast % :error "The simulator could not be deleted"))
          (ch/finally hide)))))

(defn reset-config [form sim->model id type]
  (fn [_]
    (-> (actions/reset-simulator-config id type)
        (store/dispatch)
        (ch/peek (fn [body]
                   (reset! form (sim->model (:simulator body)))
                   (toast body :success "The simulator's configuration has been reset"))
                 #(toast % :error "The simulator's configuration could not be reset")))))

(defn create-simulator [form model->source]
  (fn [_]
    (if (creatable? form)
      (let [current-model @form]
        (-> current-model
            (model->source)
            (actions/create-simulator)
            (store/dispatch)
            (ch/peek (fn [body]
                       (reset! form current-model)
                       (toast body :success "The simulator has been created")
                       (->> (select-keys (:simulator body) #{:id})
                            (nav/nav-and-replace! :details)))
                     #(toast % :error "The simulator could not be created"))))
      (ch/reject))))

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
