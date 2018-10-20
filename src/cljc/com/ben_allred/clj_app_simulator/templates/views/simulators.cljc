(ns com.ben-allred.clj-app-simulator.templates.views.simulators
  (:require [clojure.string :as string]
            [com.ben-allred.clj-app-simulator.services.navigation :as nav*]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.utils.simulators :as utils.sims]))

(defn ^:private organize [simulators]
  (->> simulators
       (remove (comp nil? :config))
       (sort-by (juxt (comp :path :config) (comp :method :config)))
       (group-by (comp :group :config))
       (sort-by key #(cond (nil? %1) 1 (nil? %2) -1 :else (compare %1 %2)))))

(defn sim-details [{{:keys [method path]} :config}]
  [:div.sim-card-identifier
   [:div.sim-card-method (some-> method (name) (string/upper-case))]
   [:div.sim-card-path
    [:span.path-prefix "/simulators"]
    [:span.path-user-defined (when (not= "/" path) path)]]])

(defn sim-card [{:keys [requests config id sockets] :as sim}]
  (let [request-count (count requests)
        socket-count (count sockets)
        {:keys [description method name]} config]
    [:li.sim-card
     [:a.details-link
      {:href (nav*/path-for :details {:id id})}
      [:div.card
       [:div.card-header
        [:div.card-header-title
         [sim-details sim]]
        [:div.sim-card-counts
         (when (pos? socket-count)
           [:div.tag.is-primary.is-rounded.tooltip.sim-card-socket-count
            {:data-tooltip "Active web socket connections"}
            socket-count])
         (when (pos? request-count)
           [:div.tag.is-info.is-rounded.tooltip.sim-card-request-count
            {:data-tooltip (if (= :ws method)
                             "Web socket messages received"
                             "Requests received")}
            request-count])]]
       (when (or name description)
         [:div.card-content
          (when name
            [:div.sim-card-name name])
          (when description
            [:div.sim-card-description
             description])])]]]))

(defn sim-group [group simulators]
  [:li.sim-group
   "Group: "
   [:span.group-name (or group "none")]
   [:ul.grouped-sims
    (for [simulator simulators]
      ^{:key (str (:id simulator))} [sim-card simulator])]])

(defn sim-section [section simulators]
  [:li.sim-section
   [:h2.title.is-3.sim-section-title (string/upper-case section)]
   [:ul
    (for [[group sims] (organize simulators)]
      ^{:key (str group)} [sim-group group sims])]])

(defn simulators [data]
  (let [sections (->> data
                      (vals)
                      (remove (comp nil? :config))
                      (group-by (comp utils.sims/config->section :config))
                      (sort-by first))]
    (if (seq data)
      [:ul
       (for [[section simulators] sections]
         ^{:key section}
         [sim-section section simulators])]
      [:p "There are no simulators. This is where you can create and manage
           your application simulators for both manual or automated testing."])))
