(ns com.ben-allred.clj-app-simulator.templates.views.core
  (:require [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [clojure.string :as string]))

(defn ^:private header-tab [path-for handler page display]
  (let [tag (if (= handler page) :span :a)]
    [tag
     (cond-> {:class-name "tab"}
       (= tag :a) (assoc :href (path-for page)))
     display]))

(defn spinner []
  [:div.loader])

(defn not-found [path-for _]
  [:div
   [:h2 "Page not found"]
   [:div
    "Try going "
    [:a.home {:href (path-for :home)} "home"]]])

(defn header [path-for {:keys [handler]}]
  [:header.header
   [:a.home-link
    {:href (path-for :home)}
    [:span.logo]]
   [header-tab path-for handler :home "simulators"]
   [header-tab path-for handler :resources "resources"]])

(defn root [children]
  (into [:div [:h2 "Simulators"]] children))

(defn details [child]
  [:div
   [:h2 "Simulator Details"]
   child])

(defn new [child state]
  (let [type (get-in state [:page :query-params :type] "http")]
    [:div
     [:h2 (str "New " (string/upper-case type) " Simulator")]
     child]))

(defn resources [child]
  [:div
   [:h2 "Resources"]
   child])

(defn app* [{:keys [header toast modal not-found components]} state]
  (let [page (:page state)
        component (components (:handler page) not-found)]
    [:div.app
     [toast state]
     [:div.scrollable
      [header page]
      [:main.main
       [component state]]]
     [modal state]]))
