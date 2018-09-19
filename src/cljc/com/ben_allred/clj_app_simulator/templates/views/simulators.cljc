(ns com.ben-allred.clj-app-simulator.templates.views.simulators
  (:require [clojure.string :as string]
            [com.ben-allred.clj-app-simulator.utils.simulators :as utils.sims]
            [com.ben-allred.clj-app-simulator.services.navigation :as nav*]))

(defn ^:private organize [simulators]
  (->> simulators
       (remove (comp nil? :config))
       (sort-by (juxt (comp :path :config) (comp :method :config)))
       (group-by (comp :group :config))
       (sort-by key #(cond (nil? %1) 1 (nil? %2) -1 :else (compare %1 %2)))))

(defn sim-details [{{:keys [method path]} :config}]
  [:div.sim-card-identifier
   [:div.sim-card-method (when method (string/upper-case (name method)))]
   [:div.sim-card-path
    [:span.path-prefix "/simulators"]
    [:span.path-user-defined (when (not= "/" path) path)]]])

(defn sim-card [{:keys [requests config id sockets] :as sim}]
  (let [request-count (count requests)
        socket-count (count sockets)]
    [:li.sim-card.pure-button.button
     [:a.clean
      {:href (nav*/path-for :details {:id id})}
      [:div.details-container
       [:div.details
        [sim-details sim]
        (when-let [name (:name config)]
          [:div.sim-card-name name])
        (when-let [description (:description config)]
          [:div.sim-card-description description])]
       [:div.sim-card-counts
        (when (pos? request-count)
          [:div.sim-card-request-count request-count])
        (when (pos? socket-count)
          [:div.sim-card-socket-count socket-count])]]]]))

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
