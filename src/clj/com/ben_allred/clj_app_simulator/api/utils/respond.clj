(ns com.ben-allred.clj-app-simulator.api.utils.respond
  (:require [com.ben-allred.clj-app-simulator.services.http :as http]))

(defn with [[status body headers]]
  (cond-> {:status 200}
    status (assoc :status (http/kw->status status status))
    body (assoc :body body)
    headers (assoc :headers headers)))
