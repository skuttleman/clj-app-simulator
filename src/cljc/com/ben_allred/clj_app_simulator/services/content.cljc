(ns com.ben-allred.clj-app-simulator.services.content
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
                     [com.ben-allred.clj-app-simulator.utils.maps :as maps]
                     [com.ben-allred.clj-app-simulator.utils.json :as json]
                     [com.ben-allred.clj-app-simulator.utils.transit :as transit]
                     [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(defn ^:private with-headers [request header-keys type]
  (update request :headers merge (zipmap header-keys (repeat type))))

(defn ^:private maybe-slurp [value]
  #?(:clj  (if (string? value)
             value
             (slurp value))
     :cljs value))

(def ^:private read-edn
  (comp edn/read-string maybe-slurp))

(defn ^:private when-not-string [body f]
  (if (string? body)
    body
    (f body)))

(def ^:private edn?
  (comp (partial re-find #"application/edn") str))

(def ^:private json?
  (comp (partial re-find #"application/json") str))

(def ^:private transit?
  (comp (partial re-find #"application/transit") str))

(defn parse [data content-type]
  (cond-> data
    (edn? content-type)
    (maps/update-maybe :body read-edn)

    (json? content-type)
    (maps/update-maybe :body json/parse)

    (transit? content-type)
    (maps/update-maybe :body transit/parse)))

(defn prepare [data header-keys accept]
  (cond
    (string? (:body data)) data
    (edn? accept) (-> data
                      (maps/update-maybe :body when-not-string pr-str)
                      (with-headers header-keys "application/edn"))
    (json? accept) (-> data
                       (maps/update-maybe :body when-not-string json/stringify)
                       (with-headers header-keys "application/json"))
    (transit? accept) (-> data
                          (maps/update-maybe :body when-not-string transit/stringify)
                          (with-headers header-keys "application/transit"))
    :else data))
