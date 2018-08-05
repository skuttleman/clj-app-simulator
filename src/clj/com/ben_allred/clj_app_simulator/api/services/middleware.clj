(ns com.ben-allred.clj-app-simulator.api.services.middleware
  (:require [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [clojure.string :as string]
            [com.ben-allred.clj-app-simulator.services.content :as content])
  (:import [java.util Date]))

(defn ^:private resource? [uri]
  (or (= "/" uri) (re-find #"(^/js|^/css|^/images|^/favicon)" uri)))

(defn ^:private api? [{:keys [uri websocket?]}]
  (and (not websocket?)
       (re-find #"(^/api|^/simulators)" uri)))

(defn log-response [handler]
  (fn [request]
    (let [start (Date.)
          response (handler request)
          end (Date.)
          uri (:uri request)]
      (when-not (resource? uri)
        (log/info (format "[%d](%dms) %s: %s"
                          (or (:status response) 404)
                          (- (.getTime end) (.getTime start))
                          (string/upper-case (name (:request-method request)))
                          uri)))
      response)))

(defn content-type [handler]
  (fn [{:keys [headers] :as request}]
    (cond-> request
      :always (content/parse (get headers "content-type"))
      :always (handler)
      (api? request) (content/prepare #{"content-type"} (get headers "accept")))))
