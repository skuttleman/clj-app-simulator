(ns com.ben-allred.clj-app-simulator.templates.simulators.ws.views
  (:require #?@(:cljs [[com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
                       [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as shared.interactions]
                       [com.ben-allred.clj-app-simulator.ui.simulators.ws.interactions :as interactions]])
            [com.ben-allred.clj-app-simulator.services.forms :as forms]
            [com.ben-allred.clj-app-simulator.templates.simulators.shared.views :as shared.views]
            [com.ben-allred.clj-app-simulator.templates.simulators.ws.resources :as resources]
            [com.ben-allred.clj-app-simulator.templates.simulators.ws.transformations :as tr]
            [com.ben-allred.clj-app-simulator.utils.attrs :as attrs]
            [com.ben-allred.clj-app-simulator.utils.datetime :as dt]))

(defn path-field [form]
  [shared.views/path-field form tr/model->view tr/view->model])

(defn name-field [form]
  [shared.views/name-field form tr/model->view tr/view->model])

(defn group-field [form]
  [shared.views/group-field form tr/model->view tr/view->model])

(defn description-field [form]
  [shared.views/description-field form tr/model->view tr/view->model])

(defn socket [simulator-id socket-id requests active?]
  (let [inactive? (not active?)]
    [:li.socket
     (attrs/classes {:active active? :inactive inactive?})
     (when (empty? requests)
       [:span.ws-messages.no-messages "no messages"])
     [:ul.ws-messages
      (for [{:keys [body timestamp]} requests]
        [:li.ws-message
         {:key (str socket-id "-" timestamp)}
         [:span.body body]
         [:span.timestamp (dt/format timestamp)]])]
     [:div.button-row
      [:button.button.button-secondary.pure-button.send-button
       {:disabled inactive?
        :on-click #?(:clj identity :cljs (interactions/show-message-modal simulator-id socket-id))}
       "Send Message"]
      [:button.button.button-error.pure-button.disconnect-button
       {:disabled inactive?
        :on-click #?(:clj identity :cljs (interactions/disconnect simulator-id socket-id))}
       "Disconnect"]]]))

(defn sim-edit-form* [id form]
  (let [disabled? (or (forms/errors form) (not (forms/changed? form)))]
    [:form.simulator-edit
     {:on-submit #?(:clj identity :cljs (interactions/update-simulator form id (not disabled?)))}
     [name-field form]
     [group-field form]
     [description-field form]
     [:div.button-row
      [:button.button.button-secondary.pure-button.save-button
       {:disabled disabled?}
       "Save"]
      [:button.button.button-warning.pure-button.reset-button
       {:type     :button
        :on-click #?(:clj identity :cljs (interactions/reset-simulator form id))}
       "Reset"]]]))

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
          [socket id socket-id requests active?])]
       [:div.no-sockets
        "None"])
     [:div.button-row
      [:button.button.button-secondary.pure-button.message-button
       {:disabled (empty? sockets)
        :on-click #?(:clj identity :cljs (interactions/show-message-modal id nil))}
       "Broadcast Message"]
      [:button.button.button-error.pure-button.clear-button
       {:disabled (empty? requests)
        :on-click #?(:clj identity :cljs (shared.interactions/clear-requests id))}
       "Clear Messages"]
      [:button.button.button-error.pure-button.disconnect-button
       {:disabled (empty? sockets)
        :on-click #?(:clj identity :cljs (interactions/disconnect-all id))}
       "Disconnect All"]
      [:button.button.button-error.pure-button.delete-button
       {:on-click #?(:clj identity :cljs (shared.interactions/show-delete-modal id))}
       "Delete Simulator"]]]))

(defn sim-create-form* [form]
  (let [disabled? (forms/errors form)]
    [:form.simulator-create
     {:on-submit #?(:clj identity :cljs (interactions/create-simulator form (not disabled?)))}
     [path-field form]
     [name-field form]
     [group-field form]
     [description-field form]
     [:div.button-row
      [:button.button.button-secondary.pure-button.save-button
       {:disabled disabled?}
       "Save"]
      [:a.button.button-warning.pure-button.reset-button
       {:href #?(:clj "#" :cljs (nav/path-for :home))}
       "Cancel"]]]))

(defn sim-create-form [_]
  (let [form (-> {:method   :ws
                  :path     "/"}
                 (forms/create resources/validate-new))]
    (fn []
      [:div.simulator
       [sim-create-form* form]])))

