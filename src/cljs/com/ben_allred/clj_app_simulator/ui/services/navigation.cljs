(ns com.ben-allred.clj-app-simulator.ui.services.navigation
  (:require [com.ben-allred.clj-app-simulator.services.navigation :as nav*]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.utils.keywords :as keywords]
            [pushy.core :as pushy]))

(defn ^:private namify [[k v]]
  [k (str (keywords/safe-name v))])

(defn reload! []
  (.reload (.-location js/window)))

(defn match-route [path]
  (nav*/match-route path))

(defn path-for
  ([page]
   (nav*/path-for page nil))
  ([page params]
   (nav*/path-for page params)))

(defonce ^:private history
  (let [history (pushy/pushy (comp store/dispatch (partial conj [:router/navigate])) match-route)]
    (pushy/start! history)
    history))

(defn navigate* [history page params]
  (pushy/set-token! history (path-for page params)))

(defn nav-and-replace* [history page params]
  (pushy/replace-token! history (path-for page params)))

(defn navigate!
  ([page] (navigate* history page nil))
  ([page params]
   (navigate* history page params)
    nil))

(defn go-to! [path]
  (set! (.-pathname (.-location js/window)) path)
  nil)

(defn nav-and-replace!
  ([page] (nav-and-replace* history page nil))
  ([page params]
   (nav-and-replace* history page params)
    nil))
