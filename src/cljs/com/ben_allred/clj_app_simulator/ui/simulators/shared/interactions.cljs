(ns com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions
  (:require [cljs.core.async :as async]
            [com.ben-allred.clj-app-simulator.ui.utils.dom :as dom]
            [com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.modals :as modals]))

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

(defn update-simulator [form model->source id submittable?]
  (fn [e]
    (let [current-model (forms/current-model form)]
      (dom/prevent-default e)
      (when submittable?
        (do-request (->> current-model
                         (model->source)
                         (actions/update-simulator id)
                         (store/dispatch))
                    (fn [_] (forms/reset! form current-model)))))))

(defn clear-requests [id]
  (fn [_]
    (do-request (store/dispatch (actions/clear-requests id)))))

(defn delete-sim
  ([id]
   (delete-sim id nil))
  ([id hide]
   (fn [_]
     (do-request (store/dispatch (actions/delete-simulator id))
                 (comp #(nav/navigate! :home) (or hide identity))))))

(defn reset-simulator [form sim->model id]
  (fn [_]
    (do-request
      (store/dispatch (actions/reset-simulator id))
      (comp (partial forms/reset! form) sim->model))))

(defn create-simulator [form model->source submittable?]
  (fn [e]
    (let [current-model (forms/current-model form)]
      (dom/prevent-default e)
      (when submittable?
        (do-request (-> current-model
                        (model->source)
                        (actions/create-simulator)
                        (store/dispatch))
                    #(nav/nav-and-replace! :details
                                           {:id (get-in % [:simulator :id])}))))))

(defn show-delete-modal [id]
  (fn [_]
    (store/dispatch
      (actions/show-modal
        [modals/confirm-delete]
        "Delete Simulator"
        [:button.button.button-error.pure-button.delete-button
         {:on-click (partial delete-sim id)}
         "Delete"]
        [:button.button.button-secondary.pure-button.cancel-button
         "Cancel"]))))
