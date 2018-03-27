(ns integration.utils.http
    (:refer-clojure :exclude [get])
    (:require [clojure.test :refer :all]
              [com.ben-allred.clj-app-simulator.services.http :as http]
              [clojure.core.async :as async]
              [integration.config :as cfg]))

(defn ^:private request* [method path content-type request]
    (async/<!! (http/request* method (cfg/->url path) (-> request
                                                          (update :headers merge {:accept       content-type
                                                                                  :content-type content-type})))))

(def success? http/success?)

(def client-error? http/client-error?)

(def server-error? http/server-error?)

(defn get [path content-type & [request]]
    (request* :get path content-type request))

(defn put [path content-type request]
    (request* :put path content-type request))

(defn post [path content-type request]
    (request* :post path content-type request))

(defn patch [path content-type request]
    (request* :patch path content-type request))

(defn delete [path content-type & [request]]
    (request* :delete path content-type request))
