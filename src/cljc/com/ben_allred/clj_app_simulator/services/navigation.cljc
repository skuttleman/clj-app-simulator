(ns com.ben-allred.clj-app-simulator.services.navigation
  (:require
    [bidi.bidi :as bidi]
    [clojure.string :as string]
    [com.ben-allred.clj-app-simulator.utils.keywords :as keywords]
    [com.ben-allred.clj-app-simulator.utils.query-params :as qp]))

(defn ^:private namify [[k v]]
  [k (str (keywords/safe-name v))])

(def ^:private routes
  ["/"
   [["" :home]
    [["details/" [#"[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}" :id]] :details]
    ["create" :new]
    ["resources" :resources]
    [true :not-found]]])

(defn match-route* [routes path]
  (let [qp (qp/parse (second (string/split path #"\?")))]
    (cond-> (bidi/match-route routes path)
      (seq qp) (assoc :query-params qp))))

(defn path-for* [routes page {:keys [query-params] :as params}]
  (let [qp (qp/stringify query-params)]
    (cond-> (apply bidi/path-for routes page (mapcat namify params))
      (seq qp) (str "?" qp))))

(defn match-route [path]
  (match-route* routes path))

(defn path-for
  ([page]
   (path-for* routes page nil))
  ([page params]
   (path-for* routes page params)))
