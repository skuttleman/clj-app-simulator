(ns com.ben-allred.clj-app-simulator.templates.views.resources
  (:require [com.ben-allred.clj-app-simulator.utils.dates :as dates]
            [com.ben-allred.clj-app-simulator.utils.fns :as fns]))

(defn resource [delete-attrs {:keys [filename timestamp]} replace-btn]
  [:li.resource
   {:style {:display :flex :justify-content :space-between}}
   [:div.resource-details
    [:div.resource-filename filename]
    [:div
     "Uploaded "
     [:span.resource-timestamp (dates/format timestamp)]]]
   [:div.button-row
    (conj replace-btn "Replace")
    [:div
     [:button.button.button-error.pure-button.delete-button
      delete-attrs
      "Delete"]]]])

(defn resources [delete-attrs upload-btn resource uploads & children]
  (into [:div
         [:div.button-row
          (conj upload-btn "Upload")
          [:div
           [:button.button.button-error.pure-button.delete-button
            delete-attrs
            "Delete All"]]]
         (if (seq uploads)
           [:ul.resources
            (for [upload (sort-by (juxt :timestamp :filename)
                                  (fns/compare-by > <)
                                  uploads)]
              ^{:key (str (:id upload))}
              [resource upload])]
           [:p "There are no uploaded resources. Once you have uploaded a
                resource, you can create a file simulator and use the
                resource as the response body."])]
        children))
