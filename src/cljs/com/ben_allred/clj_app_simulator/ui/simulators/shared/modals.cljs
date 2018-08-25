(ns com.ben-allred.clj-app-simulator.ui.simulators.shared.modals
  (:require [com.ben-allred.clj-app-simulator.utils.strings :as strings]
            [com.ben-allred.clj-app-simulator.ui.utils.moment :as mo]
            [clojure.string :as string]))

(defn sim-iterate
  ([label m class]
   (sim-iterate label m class identity))
  ([label m class xform-key]
   [:div
    {:class-name class}
    [:h5 label]
    [:ul.key-vals
     (for [[k v] (sort-by key m)]
       [:li
        {:key (str k)}
        [:span.key (xform-key (name k))]
        ": "
        [:span.val (if (coll? v)
                     (string/join "," v)
                     v)]])]]))

(defn request-modal [{:keys [method path]} {:keys [dt route-params query-params headers body]}]
  [:div.request-details
   [:p (string/upper-case (name method)) ": " path]
   [:p (mo/format dt)]
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

(defn confirm-delete [msg]
  [:div.confirm
   [:p "Are you sure you want to delete " msg "?"]])
