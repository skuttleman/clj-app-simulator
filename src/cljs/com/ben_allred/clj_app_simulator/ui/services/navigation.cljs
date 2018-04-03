(ns com.ben-allred.clj-app-simulator.ui.services.navigation
  (:require [bidi.bidi :as bidi]
            [clojure.string :as string]
            [com.ben-allred.clj-app-simulator.utils.keywords :as keywords]
            [com.ben-allred.clj-app-simulator.utils.query-params :as qp]
            [pushy.core :as pushy]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]))

(defn ^:private namify [[k v]]
  [k (str (keywords/safe-name v))])

(def ^:private routes
  ["/"
   [["" :home]
    [["details/" [#"[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}" :id]] :details]
    [true :not-found]]])

(defn match-route* [routes path]
  (let [qp (qp/parse (second (string/split path #"\?")))]
    (cond-> (bidi/match-route routes path)
            (seq qp) (assoc :query-params qp))))

(defn path-for* [routes page {:keys [query-params] :as params}]
  (let [qp (qp/stringify query-params)]
    (cond-> (apply bidi/path-for routes page (mapcat namify params))
            (seq qp) (str "?" qp))))

(defn reload! []
  (.reload (.-location js/window)))

(defn match-route [path]
  (match-route* routes path))

(defn path-for
  ([page] (path-for* routes page nil))
  ([page params] (path-for* routes page params)))

(defonce ^:private history
  (let [history (pushy/pushy (comp store/dispatch (partial conj [:router/navigate])) match-route)]
    (pushy/start! history)
    history))

(defn navigate* [history routes page params]
  (pushy/set-token! history (path-for* routes page params)))

(defn nav-and-replace* [history routes page params]
  (pushy/replace-token! history (path-for* routes page params)))

(defn navigate!
  ([page] (navigate* history routes page nil))
  ([page params]
   (navigate* history routes page params)))

(defn go-to! [path]
  (set! (.-pathname (.-location js/window)) path))

(defn nav-and-replace!
  ([page] (nav-and-replace* history routes page nil))
  ([page params]
   (nav-and-replace* history routes page params)))
