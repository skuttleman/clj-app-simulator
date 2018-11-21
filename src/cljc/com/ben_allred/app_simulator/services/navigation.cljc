(ns com.ben-allred.app-simulator.services.navigation
  (:require
    [bidi.bidi :as bidi]
    [clojure.string :as string]
    [com.ben-allred.app-simulator.utils.keywords :as keywords]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [com.ben-allred.app-simulator.utils.query-params :as qp]
    [com.ben-allred.app-simulator.utils.uuids :as uuids]))

(defn ^:private namify [[k v]]
  [k (str (keywords/safe-name v))])

(def ^:private routes
  [""
   [["/" :home]
    [["/details/" [#"[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}" :id]] :details]
    ["/create" :new]
    ["/resources" :resources]
    [true :not-found]]])

(defn ^:private re-format [{:keys [handler] :as route}]
  (cond-> route
    (= :details handler) (update-in [:route-params :id] uuids/->uuid)))

(defn ^:private sectionize [segment]
  (if (= \: (first segment))
    any?
    (partial = segment)))

(defn ^:private path->segments [path]
  (-> path
      (string/split #"/")
      (->> (remove empty?))))

(defn ^:private path->sections [path]
  (-> path
      (path->segments)
      (->> (transduce (map sectionize) conj))))

(defn path-matcher [path]
  (let [sections (path->sections path)]
    (fn [path]
      (let [segments (path->segments path)]
        (and (= (count sections) (count segments))
             (->> segments (map vector sections)
                  (every? (fn [[pred segment]] (pred segment)))))))))

(defn match-route* [routes path]
  (let [qp (qp/parse (second (string/split path #"\?")))]
    (-> routes
        (bidi/match-route path)
        (re-format)
        (cond-> (seq qp) (assoc :query-params qp)))))

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
