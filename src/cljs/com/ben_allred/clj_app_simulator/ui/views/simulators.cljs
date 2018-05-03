(ns com.ben-allred.clj-app-simulator.ui.views.simulators
  (:require [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.views :as sim]))

(defn ^:private organize [simulators]
  (->> simulators
       (vals)
       (remove nil?)
       (sort-by (juxt :path :method))
       (group-by :group)
       (sort-by key #(cond (nil? %1) 1 (nil? %2) -1 :else (compare %1 %2)))))

(defn sim-card [{:keys [requests config id] :as sim}]
  (let [request-count (count requests)]
    [:li.sim-card.pure-button.button
     {:on-click #(nav/navigate! :details {:id id})}
     [:div.details-container
      [:div.details
       [sim/sim-details sim]
       (when-let [name (:name config)]
         [:div.sim-card-name name])
       (when-let [description (:description config)]
         [:div.sim-card-description description])]
      (when (pos? request-count)
        [:div.sim-card-request-count request-count])]]))

(defn sim-group [group simulators]
  [:li.sim-group
   "Group: "
   [:span.group-name (or group "none")]
   [:ul.grouped-sims
    (for [simulator simulators]
      ^{:key (str (:id simulator))} [sim-card simulator])]])

(defn simulators [data]
  [:ul
   (for [[group sims] (organize data)]
     ^{:key (str group)} [sim-group group sims])])
