(ns com.ben-allred.clj-app-simulator.templates.views.forms.ws
  (:require #?@(:cljs [[com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]
                       [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as shared.interactions]
                       [com.ben-allred.clj-app-simulator.ui.simulators.ws.interactions :as interactions]])
    [com.ben-allred.clj-app-simulator.services.navigation :as nav*]
    [com.ben-allred.clj-app-simulator.templates.resources.ws :as resources]
    [com.ben-allred.clj-app-simulator.templates.transformations.ws :as tr]
    [com.ben-allred.clj-app-simulator.templates.views.forms.shared :as shared.views]
    [com.ben-allred.clj-app-simulator.templates.views.simulators :as views.sim]
    [com.ben-allred.clj-app-simulator.utils.dates :as dates]
    [com.ben-allred.clj-app-simulator.templates.core :as templates]))

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
     (templates/classes {:active active? :inactive inactive?})
     (when (empty? requests)
       [:span.ws-messages.no-messages "no messages"])
     [:ul.ws-messages
      (for [{:keys [body timestamp]} requests]
        [:li.ws-message
         {:key (str socket-id "-" timestamp)}
         [:span.body body]
         [:span.timestamp (dates/format timestamp)]])]
     [:div.button-row
      [:button.button.button-secondary.pure-button.send-button
       #?(:clj  {:disabled true}
          :cljs {:disabled inactive?
                 :on-click (interactions/show-message-modal simulator-id socket-id)})
       "Send Message"]
      [:button.button.button-error.pure-button.disconnect-button
       #?(:clj  {:disabled true}
          :cljs {:disabled inactive?
                 :on-click (interactions/disconnect simulator-id socket-id)})
       "Disconnect"]]]))

(defn sim-edit-form* [id form]
  (let [disabled? #?(:clj true :cljs (or (forms/errors form) (not (forms/changed? form))))]
    [:form.simulator-edit
     #?(:cljs {:on-submit (interactions/update-simulator form id (not disabled?))})
     [name-field form]
     [group-field form]
     [description-field form]
     [:div.button-row
      [:button.button.button-secondary.pure-button.save-button
       {:disabled disabled?}
       "Save"]
      [:button.button.button-warning.pure-button.reset-button
       {:type :button
        #?@(:clj  [:disabled true]
            :cljs [:on-click (interactions/reset-simulator form id)])}
       "Reset"]]]))

(defn sim-edit-form [{:keys [id] :as sim}]
  (let [model (tr/sim->model sim)
        form #?(:clj  model
                :cljs (forms/create model))]
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
     [views.sim/sim-details simulator]
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
       #?(:clj  {:disabled true}
          :cljs {:disabled (empty? sockets)
                 :on-click (interactions/show-message-modal id nil)})
       "Broadcast Message"]
      [:button.button.button-error.pure-button.clear-button
       #?(:clj  {:disabled true}
          :cljs {:disabled (empty? requests)
                 :on-click (shared.interactions/clear-requests :ws id)})
       "Clear Messages"]
      [:button.button.button-error.pure-button.disconnect-button
       #?(:clj  {:disabled true}
          :cljs {:disabled (empty? sockets)
                 :on-click (interactions/disconnect-all id)})
       "Disconnect All"]
      [:button.button.button-error.pure-button.delete-button
       #?(:clj  {:disabled true}
          :cljs {:on-click (shared.interactions/show-delete-modal id)})
       "Delete Simulator"]]]))

(defn sim-create-form* [form]
  (let [disabled? #?(:clj true :cljs (forms/errors form))]
    [:form.simulator-create
     #?(:cljs {:on-submit (interactions/create-simulator form (not disabled?))})
     [path-field form]
     [name-field form]
     [group-field form]
     [description-field form]
     [:div.button-row
      [:button.button.button-secondary.pure-button.save-button
       {:disabled disabled?}
       "Save"]
      [:a.button.button-warning.pure-button.reset-button
       {:href (nav*/path-for :home)}
       "Cancel"]]]))

(defn sim-create-form []
  (let [model {:method :ws
               :path   "/"}
        form #?(:clj  model
                :cljs (forms/create model resources/validate-new))]
    (fn []
      [:div.simulator
       [sim-create-form* form]])))

