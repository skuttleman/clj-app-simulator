(ns com.ben-allred.app-simulator.ui.simulators.shared.modals
  (:require
    [clojure.string :as string]
    [com.ben-allred.app-simulator.utils.dates :as dates]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [com.ben-allred.app-simulator.utils.strings :as strings]
    [com.ben-allred.app-simulator.templates.views.forms.shared :as shared.views]
    [com.ben-allred.app-simulator.templates.fields :as fields]))

(defn sim-iterate
  ([label m class]
   (sim-iterate label m class identity))
  ([label m class xform-key]
   [:div
    {:class-name class}
    [:h4.title.is-5 label]
    [:ul.key-vals
     (for [[k v] (sort-by key m)]
       [:li
        {:key (str k)}
        [:span.key (xform-key (name k))]
        ": "
        [:span.val (if (coll? v)
                     (string/join "," v)
                     v)]])]]))

(defn request-modal [{:keys [method path]} {:keys [timestamp route-params query-params headers body]}]
  [:div.request-details
   [:p (string/upper-case (name method)) (str ": /simulators" (when (not= "/" path) path))]
   [:p (dates/format timestamp)]
   (when (seq route-params)
     [sim-iterate "Route Params:" route-params "route-params"])
   (when (seq query-params)
     [sim-iterate "Query:" query-params "query-params"])
   (when (seq headers)
     [sim-iterate "Headers:" headers "headers" strings/titlize])
   (when (seq body)
     [:div.request-body
      [:span "Body:"]
      [:p body]])])

(defn socket-modal [{:keys [timestamp body]}]
  [:div.message-details
   [:p (dates/format timestamp)]
   [:div.socket-message-body
    [:span "Body:"]
    [:pre.message-body body]]])

(defn confirm-delete [msg]
  [:div.confirm
   [:p "Are you sure you want to delete " msg "?"]])

(defn message-editor [form model->view view->model]
  [:div.send-ws-message
   [fields/textarea
    (-> {:label "Message"}
        (shared.views/with-attrs form [:message] model->view view->model))]])
