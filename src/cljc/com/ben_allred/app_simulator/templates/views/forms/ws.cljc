(ns com.ben-allred.app-simulator.templates.views.forms.ws
  (:require
    #?@(:cljs [[com.ben-allred.app-simulator.ui.services.forms.standard :as form.std]
               [com.ben-allred.app-simulator.ui.simulators.shared.interactions :as shared.interactions]
               [com.ben-allred.app-simulator.ui.simulators.ws.interactions :as interactions]
               [reagent.core :as r]])
    [com.ben-allred.app-simulator.services.forms.noop :as form.no]
    [com.ben-allred.app-simulator.services.navigation :as nav*]
    [com.ben-allred.app-simulator.templates.resources.ws :as resources]
    [com.ben-allred.app-simulator.templates.transformations.ws :as tr]
    [com.ben-allred.app-simulator.templates.views.forms.shared :as shared.views]
    [com.ben-allred.app-simulator.templates.views.simulators :as views.sim]
    [com.ben-allred.app-simulator.utils.dates :as dates]
    [com.ben-allred.app-simulator.templates.core :as templates]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [com.ben-allred.app-simulator.templates.fields :as fields]))

(defn ^:private with-attrs [attrs form path]
  (shared.views/with-attrs attrs form path tr/model->view tr/view->model))

(defn path-field [form]
  [fields/input
   (-> {:label       "Path"
        :auto-focus? true}
       (with-attrs form [:path]))])

(defn name-field
  ([form]
   (name-field form false))
  ([form auto-focus?]
   [fields/input
    (-> {:label       "Name"
         :auto-focus? auto-focus?}
        (with-attrs form [:name]))]))

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
      (for [{:keys [body timestamp id] :as msg} requests]
        [:li.ws-message
         {:key (str id)}
         [:div.ws-content.info
          #?(:cljs {:on-click (interactions/show-ws-modal msg)})
          [:p.ws-body body]
          [:span.timestamp (dates/format timestamp)]]])]
     [:div.button-row
      [:button.button.is-info.send-button
       #?(:clj  {:disabled true}
          :cljs {:disabled inactive?
                 :on-click (interactions/show-send-modal simulator-id socket-id)})
       "Send Message"]
      [:button.button.is-danger.disconnect-button
       #?(:clj  {:disabled true}
          :cljs {:disabled inactive?
                 :on-click (interactions/disconnect simulator-id socket-id)})
       "Disconnect"]]]))

(defn sim-edit-form* [id form]
  [:form.simulator-edit
   #?(:cljs (-> {:on-submit (interactions/update-simulator form id)}
                (shared.views/with-sync-action form :on-submit)))
   [name-field form true]
   [group-field form]
   [description-field form]
   [:div.button-row
    [shared.views/sync-button
     {:form       form
      :text       "Save"
      :sync-text  "Saving"
      :class-name "is-info save-button"
      :disabled   #?(:clj true :cljs (shared.views/edit-disabled? form))}]
    [shared.views/sync-button
     {:form       form
      :text       "Reset"
      :sync-text  "Resetting"
      :type       :button
      :class-name "is-warning reset-button"
      :disabled   #?(:clj true :cljs false)
      #?@(:cljs [:on-click (interactions/reset-simulator id)
                 :on-event :on-click])}]]])

(defn sim-edit-form [{:keys [id] :as sim}]
  (let [model (tr/source->model (:config sim))
        form #?(:clj  (form.no/create model)
                :cljs (form.std/create model))]
    (fn [_simulator]
      [sim-edit-form* id form])))

(defn sim [{:keys [id sockets requests] :as simulator}]
  (let [connections (->> requests
                         (group-by :socket-id)
                         (merge (zipmap sockets (repeat [])))
                         (map (fn [[socket-id requests]]
                                [socket-id {:requests requests :active? (contains? sockets socket-id)}]))
                         (sort-by (juxt (comp not :active? second) (comp :timestamp first :requests))))]
    [:div.simulator
     [views.sim/sim-details simulator]
     [sim-edit-form simulator]
     [:h3.title.is-4 "Connections:"]
     (if (seq connections)
       [:ul.sockets
        (for [[socket-id {:keys [active? requests]}] connections]
          ^{:key (str socket-id)}
          [socket id socket-id requests active?])]
       [:div.no-sockets
        "None"])
     [:div.button-row
      [:button.button.is-info.message-button
       #?(:clj  {:disabled true}
          :cljs {:disabled (empty? sockets)
                 :on-click (interactions/show-send-modal id nil)})
       "Broadcast Message"]
      [:button.button.is-danger.clear-button
       #?(:clj  {:disabled true}
          :cljs {:disabled (empty? requests)
                 :on-click (shared.interactions/clear-requests :ws id)})
       "Clear Messages"]
      [:button.button.is-danger.disconnect-button
       #?(:clj  {:disabled true}
          :cljs {:disabled (empty? sockets)
                 :on-click (interactions/disconnect-all id)})
       "Disconnect All"]
      [:button.button.is-danger.delete-button
       #?(:clj  {:disabled true}
          :cljs {:on-click (shared.interactions/show-delete-modal id)})
       "Delete Simulator"]]]))

(defn sim-create-form* [form]
  [:form.simulator-create
   #?(:cljs (-> {:on-submit (interactions/create-simulator form)}
                (shared.views/with-sync-action form :on-submit)))
   [path-field form]
   [name-field form]
   [group-field form]
   [description-field form]
   [:div.button-row
    [shared.views/sync-button
     {:form       form
      :text       "Save"
      :sync-text  "Saving"
      :disabled   #?(:clj true :cljs (shared.views/create-disabled? form))
      :class-name "is-info save-button"}]
    [:a.button.is-warning.reset-button
     {:href (nav*/path-for :home)}
     "Cancel"]]])

(defn sim-create-form []
  (let [model {:method :ws/ws
               :path   "/"}
        form #?(:clj  (form.no/create model)
                :cljs (form.std/create model resources/validate-new))]
    (fn []
      [:div.simulator
       [sim-create-form* form]])))

