(ns com.ben-allred.clj-app-simulator.templates.views.simulators
  (:require #?(:cljs [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav])
            [clojure.string :as string]
            [com.ben-allred.clj-app-simulator.templates.components.core :as components]
            [com.ben-allred.clj-app-simulator.templates.simulators.shared.views :as shared.views]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.utils.simulators :as utils.sims]))

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
     {:on-click #?(:clj identity :cljs #(nav/navigate! :details {:id id}))}
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

(defn simulators [home-welcome? uploads data]
  (let [sections (->> data
                      (vals)
                      (remove (comp nil? :config))
                      (group-by (comp utils.sims/config->section :config))
                      (sort-by first))]
    [:div
     [:div.button-row
      [components/menu
       {:items (cond->> [[:http "HTTP Simulator"] [:ws "WS Simulator"]]
                 (seq (:data uploads)) (cons [:file "File Server"])
                 :always (map (fn [[type label]]
                                {:href  #?(:clj "#" :cljs (nav/path-for :new {:query-params {:type type}}))
                                 :label label})))}
       [:button.button.button-success.pure-button "Create"]]]
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
       [:p "There are no simulators."])]))

