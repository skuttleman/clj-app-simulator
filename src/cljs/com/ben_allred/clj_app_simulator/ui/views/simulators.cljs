(ns com.ben-allred.clj-app-simulator.ui.views.simulators
  (:require [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.ui.views.components.core :as components]
            [clojure.string :as string]
            [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]))

(defn ^:private organize [simulators]
  (->> simulators
       (vals)
       (remove nil?)
       (sort-by (juxt :path :method))
       (group-by :group)
       (sort-by key #(cond (nil? %1) 1 (nil? %2) -1 :else (compare %1 %2)))))

(defn sim-card [{:keys [path method requests description id] :as sim}]
  (let [request-count (count requests)]
    [:li.sim-card.pure-button.button
     {:on-click #(nav/navigate! :details {:id id})}
     [:div.details-container
      [:div.details
       [:div.sim-card-identifier
        [:div.sim-card-method (string/upper-case (name method))]
        [:div.sim-card-path path]]
       (when-let [name (:name sim)]
         [:div.sim-card-name name])
       (when description
         [:div.sim-card-description description])]
      (when (pos? request-count)
        [:div.sim-card-request-count request-count])]]))

(defn sim-group [group simulators]
  [:li.sim-group
   "Group: "
   [:span.group-name (or group "none")]
   [:ul.grouped-sims
    (for [{:keys [id] :as simulator} simulators]
      ^{:key (str id)} [sim-card simulator])]])

(defn simulators [_simulators]
  (store/dispatch actions/request-simulators)
  (fn [{:keys [status data]}]
    (cond
      (= :init status)
      [components/spinner]

      (not= :available status)
      [:div "Something's wrong"]

      :else
      [:ul
       (for [[group sims] (organize data)]
         ^{:key (str group)} [sim-group group sims])])))
