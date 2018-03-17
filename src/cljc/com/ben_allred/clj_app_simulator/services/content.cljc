(ns com.ben-allred.clj-app-simulator.services.content
    (:require [com.ben-allred.clj-app-simulator.utils.maps :as maps]
              [com.ben-allred.clj-app-simulator.utils.json :as json]
              [com.ben-allred.clj-app-simulator.utils.transit :as transit]
              [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(def ^:private header-keys
    #?(:clj  #{"content-type" "accept"}
       :cljs #{:content-type :accept}))

(defn ^:private with-headers [request type]
    (update request :headers merge (zipmap header-keys (repeat type))))

(def ^:private read-edn
    #?(:clj  clojure.edn/read-string
       :cljs cljs.reader/read-string))

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

(defn prepare [data accept]
    (if (string? (:body data))
        data
        (cond-> data
            (edn? accept) (->
                              (maps/update-maybe :body when-not-string pr-str)
                              (with-headers "application/edn"))
            (json? accept) (->
                               (maps/update-maybe :body when-not-string json/stringify)
                               (with-headers "application/json"))
            (transit? accept) (->
                                  (maps/update-maybe :body when-not-string transit/stringify)
                                  (with-headers "application/transit")))))
