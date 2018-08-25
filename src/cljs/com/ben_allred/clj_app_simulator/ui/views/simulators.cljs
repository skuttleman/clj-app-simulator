(ns com.ben-allred.clj-app-simulator.ui.views.simulators
  (:require [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
            [com.ben-allred.clj-app-simulator.ui.utils.simulators :as utils.sims]
            [clojure.string :as string]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.views :as shared.views]))

(defn ^:private organize [simulators]
  (->> simulators
       (remove (comp nil? :config))
       (sort-by (juxt (comp :path :config) (comp :method :config)))
       (group-by (comp :group :config))
       (sort-by key #(cond (nil? %1) 1 (nil? %2) -1 :else (compare %1 %2)))))

(defn sim-card [{:keys [requests config id sockets] :as sim}]
  (let [request-count (count requests)
        socket-count (count sockets)]
    [:li.sim-card.pure-button.button
     {:on-click #(nav/navigate! :details {:id id})}
     [:div.details-container
      [:div.details
       [shared.views/sim-details sim]
       (when-let [name (:name config)]
         [:div.sim-card-name name])
       (when-let [description (:description config)]
         [:div.sim-card-description description])]
      [:div.sim-card-counts
       (when (pos? request-count)
         [:div.sim-card-request-count request-count])
       (when (pos? socket-count)
         [:div.sim-card-socket-count socket-count])]]]))

(defn sim-group [group simulators]
  [:li.sim-group
   "Group: "
   [:span.group-name (or group "none")]
   [:ul.grouped-sims
    (for [simulator simulators]
      ^{:key (str (:id simulator))} [sim-card simulator])]])

(defn sim-section [section simulators]
  [:li.sim-section
   [:h3.sim-section-title (string/upper-case section)]
   [:ul
    (for [[group sims] (organize simulators)]
      ^{:key (str group)} [sim-group group sims])]])

(defn simulators [home-welcome? data]
  (let [sections (->> data
                      (vals)
                      (remove (comp nil? :config))
                      (group-by (comp utils.sims/config->section :config))
                      (sort-by first))]
    (cond
      home-welcome?
      [:p "Welcome. This is where you can create and manage your application
           simulators for both manual or automated testing."]

      (seq data)
      [:ul
       (for [[section simulators] sections]
         ^{:key section}
         [sim-section section simulators])]

      :else
      [:p "There are no simulators."])))
