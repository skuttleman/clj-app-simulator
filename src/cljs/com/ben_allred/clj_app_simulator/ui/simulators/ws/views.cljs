(ns com.ben-allred.clj-app-simulator.ui.simulators.ws.views
  (:require [com.ben-allred.clj-app-simulator.ui.utils.core :as utils]
            [com.ben-allred.clj-app-simulator.ui.utils.moment :as mo]
            [com.ben-allred.clj-app-simulator.ui.simulators.ws.resources :as resources]
            [com.ben-allred.clj-app-simulator.ui.simulators.ws.transformations :as tr]
            [com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.views :as shared.views]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as shared.interactions]
            [com.ben-allred.clj-app-simulator.ui.simulators.ws.interactions :as interactions]))

(defn name-field [form]
  [shared.views/name-field form tr/model->view tr/view->model])

(defn group-field [form]
  [shared.views/group-field form tr/model->view tr/view->model])

(defn description-field [form]
  [shared.views/description-field form tr/model->view tr/view->model])

(defn socket [socket-id requests active?]
  [:li.socket
   (utils/classes {:active active? :inactive (not active?)})
   (when (empty? requests)
     [:span.ws-messages.no-messages "no messages"])
   [:ul.ws-messages
    (for [{:keys [body timestamp]} requests]
      [:li.ws-message
       {:key (str socket-id "-" timestamp)}
       body
       (mo/format (mo/->moment timestamp))])]])

(defn sim-edit-form* [id form]
  (let [disabled? (or (forms/errors form) (not (forms/changed? form)))]
    [:form.simulator-edit
     {:on-submit (interactions/update-simulator form id (not disabled?))}
     [name-field form]
     [group-field form]
     [description-field form]
     [:div.button-row
      [:button.button.button-warning.pure-button.reset-button
       {:type     :button
        :on-click (interactions/reset-simulator form id)}
       "Reset"]
      [:button.button.button-secondary.pure-button.save-button
       {:disabled disabled?}
       "Save"]]]))

(defn sim-edit-form [{:keys [id] :as sim}]
  (let [form (-> sim
                 (tr/sim->model)
                 (forms/create))]
    (fn [_simulator]
      [sim-edit-form* id form])))

(defn sim [{:keys [sockets requests id] :as simulator}]
  (let [connections (->> requests
                         (group-by :socket-id)
                         (merge (zipmap sockets (repeat [])))
                         (map (fn [[socket-id requests]]
                                [socket-id {:requests requests :active? (contains? sockets socket-id)}]))
                         (sort-by (juxt (comp not :active? second) (comp :timestamp first :requests))))]
    [:div.simulator
     [shared.views/sim-details simulator]
     [sim-edit-form simulator]
     [:h4 "Connections:"]
     (if (seq connections)
       [:ul.sockets
        (for [[socket-id {:keys [active? requests]}] connections]
          ^{:key (str socket-id)}
          [socket socket-id requests active?])]
       [:div.no-sockets
        "None"])
     [:div.button-row
      [:button.button.button-error.pure-button.clear-button
       {:disabled (empty? requests)
        :on-click (shared.interactions/clear-requests id)}
       "Clear Messages"]
      [:button.button.button-error.pure-button.disconnect-button
       {:disabled (empty? sockets)
        :on-click (interactions/disconnect-all id)}
       "Disconnect All"]
      [:button.button.button-error.pure-button.delete-button
       {:on-click (shared.interactions/show-delete-modal id)}
       "Delete Simulator"]]]))
