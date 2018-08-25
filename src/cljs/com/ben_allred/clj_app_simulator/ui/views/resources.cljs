(ns com.ben-allred.clj-app-simulator.ui.views.resources
  (:require [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.views.components.core :as components]
            [com.ben-allred.clj-app-simulator.ui.utils.moment :as mo]
            [com.ben-allred.clj-app-simulator.utils.fns :as fns]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.modals :as modals]))

(defn show-delete-modal [title msg action]
  (fn [_]
    (store/dispatch
      (actions/show-modal
        [modals/confirm-delete msg]
        title
        [:button.button.button-error.pure-button.delete-button
         {:on-click (fn [hide]
                      (fn [_]
                        (hide)
                        (store/dispatch action)))}
         "Delete"]
        [:button.button.button-secondary.pure-button.cancel-button
         "Cancel"]))))

(defn resource [{:keys [id filename timestamp]}]
  [:li.resource
   {:style {:display :flex :justify-content :space-between}}
   [:div.resource-details
    [:div.resource-filename filename]
    [:div
     "Uploaded "
     [:span.resource-timestamp (mo/format (mo/->moment timestamp))]]]
   [:div.button-row
    [components/upload
     {:on-change  (comp store/dispatch (partial actions/upload-replace id))
      :class-name "button button-warning pure-button"
      :multiple false}
     "Replace"]
    [:div
     [:button.button.button-error.pure-button.delete-button
      {:on-click (show-delete-modal "Delete Resource" "this resource" (actions/delete-upload id))}
      "Delete"]]]])

(defn root [upload-welcome? uploads]
  [:div
   [:div.button-row
    [components/upload
     {:on-change  (comp store/dispatch actions/upload)
      :class-name "button button-success pure-button"}
     "Upload"]
    [:div
     [:button.button.button-error.pure-button.delete-button
      {:disabled (empty? uploads)
       :on-click (show-delete-modal "Delete All Resources" "all resources" actions/delete-uploads)}
      "Delete All"]]]
   (cond
     upload-welcome?
     [:p "This is where you can upload and manage static resources. Once you have uploaded a resource,
          you can create a file simulator and use the resource as the response body."]

     (seq uploads)
     [:ul.resources
      (for [upload (sort-by (juxt :timestamp :filename)
                            (fns/compare-by > <)
                            uploads)]
        ^{:key (str (:id upload))}
        [resource upload])]

     :else
     [:p "There are no uploaded resources."])])
