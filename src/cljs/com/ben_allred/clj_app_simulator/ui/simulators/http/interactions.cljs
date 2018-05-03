(ns com.ben-allred.clj-app-simulator.ui.simulators.http.interactions
  (:require [com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]
            [com.ben-allred.clj-app-simulator.ui.utils.dom :as dom]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [cljs.core.async :as async]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.modals :as modals]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.transformations :as tr]
            [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]))

(defn do-request
  ([request!]
   (do-request request! identity))
  ([request! on-success]
   (do-request request! on-success identity))
  ([request! on-success on-failure]
   (async/go
     (let [[status body] (async/<! (request!))]
       (if (= :success status)
         (on-success body)
         (on-failure body))))))

(defn update-simulator [form id submittable?]
  (fn [e]
    (let [current-model (forms/current-model form)]
      (dom/prevent-default e)
      (when submittable?
        (do-request #(->> current-model
                          (tr/model->source)
                          (actions/update-simulator id)
                          (store/dispatch))
                    (fn [_] (forms/reset! form current-model)))))))

(defn reset-simulator [form id]
  (fn [_]
    (do-request #(store/dispatch (actions/reset-simulator id))
                (comp (partial forms/reset! form) tr/sim->model))))

(defn clear-requests [id]
  (fn [_]
    (do-request #(store/dispatch (actions/clear-requests id)))))

(defn delete-sim
  ([id]
   (delete-sim id nil))
  ([id hide]
   (fn [_]
     (do-request #(store/dispatch (actions/delete-simulator id))
                 (comp #(nav/navigate! :home) (or hide identity))))))

(defn show-delete-modal [id]
  (fn [_]
    (store/dispatch
      (actions/show-modal
        [modals/confirm-delete]
        "Delete Simulator"
        [:button.button.button-secondary.pure-button
         "Cancel"]
        [:button.button.button-error.pure-button
         {:on-click (partial delete-sim id)}
         "Delete"]))))

(defn show-request-modal [sim request dt]
  (fn [_]
    (store/dispatch
      (actions/show-modal
        [modals/request-modal sim (assoc request :dt dt)]
        "Request Details"))))
