(ns com.ben-allred.clj-app-simulator.templates.views.core
  (:require
    [clojure.string :as string]
    [com.ben-allred.clj-app-simulator.services.navigation :as nav*]
    [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(defn ^:private header-tab [handler page display]
  (let [tag (if (= handler page) :span :a)]
    [tag
     (cond-> {:class-name "tab"}
       (= tag :a) (assoc :href (nav*/path-for page)))
     display]))

(defn spinner []
  [:div.loader])

(defn not-found [_]
  [:div
   [:h1.title.is-2 "Page not found"]
   [:div
    "Try going "
    [:a.home {:href (nav*/path-for :home)} "home"]]])

(defn header [{:keys [handler]}]
  [:header.header
   [:a.home-link
    {:href (nav*/path-for :home)}
    [:span.logo]]
   [header-tab handler :home "simulators"]
   [header-tab handler :resources "resources"]])

(defn root [& children]
  (into [:div [:h1.title.is-2 "Simulators"]] children))

(defn details [& children]
  (into [:div [:h1.title.is-2 "Simulator Details"]] children))

(defn new [state & children]
  (let [type (get-in state [:page :query-params :type] "http")]
    (into [:div
           [:h1.title.is-2 (str "New " (string/upper-case type) " Simulator")]]
          children)))

(defn resources [child]
  [:div
   [:h1.title.is-2 "Resources"]
   child])

(defn app* [{:keys [toast modal components]} state]
  (let [page (:page state)
        component (components (:handler page) not-found)]
    [:div.app
     [toast state]
     [:div.scrollable
      [header page]
      [:main.main
       [component state]]]
     [modal state]]))
