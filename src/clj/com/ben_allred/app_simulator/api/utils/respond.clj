(ns com.ben-allred.app-simulator.api.utils.respond
  (:require
    [com.ben-allred.app-simulator.services.http :as http]))

(defn ^:private type->response [type data]
  (case type
    :resources/empty
    [:http.status/bad-request
     {:message (format "Must supply a file for the form param `%s`"
                       (:param data))}]

    :resources/failed-spec
    [:http.status/bad-request
     {:message (format "Bad value submitted for the form param `%s`"
                       (:param data))}]

    :simulators.init/failed-spec
    [:http.status/bad-request
     {:message "One or more invalid simulator specifications"}]

    :simulators.add/failed-spec
    [:http.status/bad-request
     {:message "Invalid simulator specification"}]

    :simulators.change/failed-spec
    [:http.status/bad-request
     {:message "Invalid simulator change specification"}]

    :simulators.add/duplicate-sim
    [:http.status/bad-request
     {:message "A simulator already exists for this path and method"}]

    [:http.status/internal-server-error]))

(defn with [[status body headers]]
  (cond-> {:status 200}
    status (assoc :status (http/kw->status status status))
    body (assoc :body body)
    headers (assoc :headers headers)))

(defn abort!
  ([type]
   (abort! type nil))
  ([type data]
   (throw (ex-info "Abort!"
                   {:type     :http/failure
                    :response (with (type->response type data))}))))
