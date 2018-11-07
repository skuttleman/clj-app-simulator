(ns integration.utils.http
  (:refer-clojure :exclude [get])
  (:require
    [clojure.core.async :as async]
    [clojure.java.io :as io]
    [clojure.test :refer :all]
    [com.ben-allred.app-simulator.services.files :as files]
    [com.ben-allred.app-simulator.services.http :as http]
    [integration.utils.api :as test.api]
    [integration.config :as cfg]))

(defn ^:private request* [method path content-type request]
  (async/<!! (http/go method (cfg/->url path) (-> request
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

(defn upload [path content-type & fixtures]
  (-> path
      (cfg/->url)
      (files/upload :post (map (comp :file test.api/fixture->file) fixtures) content-type "text/plain")
      (async/<!!)))

(defn upload-put [path content-type fixture]
  (-> path
      (cfg/->url)
      (files/upload :put [(:file (test.api/fixture->file fixture))] content-type "text/plain")
      (async/<!!)))
