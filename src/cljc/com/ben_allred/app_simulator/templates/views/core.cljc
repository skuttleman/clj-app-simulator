(ns com.ben-allred.app-simulator.templates.views.core
  (:require
    #?@(:cljs [[com.ben-allred.app-simulator.ui.services.store.actions :as actions]
               [com.ben-allred.app-simulator.ui.services.store.core :as store]
               [com.ben-allred.app-simulator.ui.utils.dom :as dom]])
    [clojure.string :as string]
    [com.ben-allred.app-simulator.services.navigation :as nav*]
    [com.ben-allred.app-simulator.templates.core :as templates]
    [com.ben-allred.app-simulator.utils.logging :as log]))

(defn ^:private header-tab
  ([active? page display]
   (header-tab nil active? page display))
  ([attrs active? page display]
   [:li.tab
    (-> attrs
        (templates/classes {:is-active active?}))
    [:a
     {:href (nav*/path-for page)}
     display]]))

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
   [:div.tabs.is-boxed
    [:ul
     [header-tab {:class-name "home-link"} false :home [:span.logo]]
     [header-tab (= :home handler) :home "Simulators"]
     [header-tab (= :resources handler) :resources "Resources"]]]])

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
     #?(:cljs {:on-key-down-capture (fn [e]
                                      (when (and (= :key-codes/esc (dom/event->key e))
                                                 (not= (get-in state [:modal :state]) :unmounted))
                                        (store/dispatch actions/hide-modal)))})
     [toast state]
     [:div.scrollable
      [header page]
      [:main.main
       [component state]]]
     [modal state]]))
