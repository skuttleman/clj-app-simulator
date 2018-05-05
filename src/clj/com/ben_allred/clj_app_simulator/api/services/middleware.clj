(ns com.ben-allred.clj-app-simulator.api.services.middleware
  (:require [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [clojure.string :as string]
            [com.ben-allred.clj-app-simulator.services.content :as content]))

(defn ^:private resource? [uri]
  (or (= "/" uri) (re-find #"(^/js|^/css|^/images|^/favicon)" uri)))

(defn ^:private api? [uri]
  (re-find #"(^/api|^/simulators)" uri))

(defn log-response [handler]
  (fn [request]
    (let [response (handler request)
          uri (:uri request)]
      (when-not (resource? uri)
        (log/info (format "[%d] %s: %s"
                          (or (:status response) 404)
                          (string/upper-case (name (:request-method request)))
                          uri)))
      response)))

(defn content-type [handler]
  (fn [request]
    (let [{:keys [uri headers]} request]
      (cond-> request
        :always (content/parse (get headers "content-type"))
        :always (handler)
        (api? uri) (content/prepare #{"content-type" "accept"} (get headers "accept"))))))
