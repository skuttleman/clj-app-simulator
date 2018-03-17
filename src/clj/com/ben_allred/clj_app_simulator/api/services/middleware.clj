(ns com.ben-allred.clj-app-simulator.api.services.middleware
    (:require [com.ben-allred.clj-app-simulator.utils.logging :as log]
              [clojure.string :as string]
              [com.ben-allred.clj-app-simulator.services.content :as content]
              [com.ben-allred.clj-app-simulator.utils.maps :as maps]))

(defn ^:private resource? [uri]
    (or (= "/" uri) (re-find #"(^/js|^/css|^/images)" uri)))

(defn log-response [handler]
    (fn [request]
        (let [response (handler request)
              uri      (:uri request)]
            (when-not (resource? uri)
                (log/info (format "[%d] %s: %s"
                              (or (:status response) 404)
                              (string/upper-case (name (:request-method request)))
                              uri)))
            response)))

(defn content-type [handler]
    (fn [request]
        (let [headers (:headers request)]
            (-> request
                (content/parse (get headers "content-type"))
                (handler)
                (content/prepare (get headers "accept"))))))
