(ns com.ben-allred.clj-app-simulator.api.utils.response
    (:require [com.ben-allred.clj-app-simulator.services.http :as http]))

(defn respond [[status body headers]]
    (cond-> {:status 200}
        status (assoc :status (http/kw->status status status))
        body (assoc :body body)
        headers (assoc :headers headers)))
