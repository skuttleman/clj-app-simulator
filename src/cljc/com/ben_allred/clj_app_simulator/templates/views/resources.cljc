(ns com.ben-allred.clj-app-simulator.templates.views.resources
  (:require
    [com.ben-allred.clj-app-simulator.utils.dates :as dates]
    [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(defn resource [delete-attrs {:keys [filename timestamp]} replace-btn]
  [:li.resource
   {:style {:display :flex :justify-content :space-between}}
   [:div.resource-details
    [:div.resource-filename filename]
    [:div
     "Uploaded "
     [:span.resource-timestamp (dates/format timestamp)]]]
   [:div.button-row.buttons-right
    (conj replace-btn "Replace")
    [:div
     [:button.button.is-danger
      delete-attrs
      "Delete"]]]])

(defn resources [delete-attrs upload-btn resource uploads & children]
  (into [:div
         [:div.button-row
          (conj upload-btn "Upload")
          [:div
           [:button.button.is-danger
            delete-attrs
            "Delete All"]]]
         (if (seq uploads)
           [:ul.resources
            (for [upload (sort-by (juxt :timestamp :filename)
                                  (fn [[ts1 f1] [ts2 f2]]
                                    (let [ts (compare ts1 ts2)]
                                      (if (zero? ts)
                                        (compare f1 f2)
                                        (* -1 ts))))
                                  uploads)]
              ^{:key (str (:id upload))}
              [resource upload])]
           [:p "There are no uploaded resources. Once you have uploaded a
                resource, you can create a file simulator and use the
                resource as the response body."])]
        children))
