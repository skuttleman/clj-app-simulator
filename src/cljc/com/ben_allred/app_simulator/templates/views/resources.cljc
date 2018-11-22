(ns com.ben-allred.app-simulator.templates.views.resources
  (:require
    [com.ben-allred.app-simulator.utils.dates :as dates]
    [com.ben-allred.app-simulator.utils.logging :as log]))

(defn resource [delete-attrs {:keys [filename timestamp]} replace-btn]
  [:li.resource
   {:style {:display :flex :justify-content :space-between :align-items :center}}
   [:div.resource-details
    [:div.resource-filename filename]
    [:div
     "Uploaded "
     [:span.resource-timestamp (dates/format timestamp)]]]
   [:div.button-row.buttons-right
    replace-btn
    [:div
     [:button.button.is-danger
      delete-attrs
      "Delete"]]]])

(defn resources [delete-attrs upload-btn resource resources & children]
  (into [:div
         [:div.button-row
          upload-btn
          [:div
           [:button.button.is-danger
            delete-attrs
            "Delete All"]]]
         (if (seq resources)
           [:ul.resources
            (for [upload (sort-by (juxt :timestamp :filename)
                                  (fn [[ts1 f1] [ts2 f2]]
                                    (let [ts (compare ts1 ts2)]
                                      (if (zero? ts)
                                        (compare f1 f2)
                                        (* -1 ts))))
                                  resources)]
              ^{:key (str (:id upload))}
              [resource upload])]
           [:p "There are no uploaded resources. Once you have uploaded a
                resource, you can create a file simulator and use the
                resource as the response body."])]
        children))
