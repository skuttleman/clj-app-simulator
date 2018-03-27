(ns com.ben-allred.clj-app-simulator.api.services.simulators.store.actions
  (:require [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.utils.maps :as maps]
            [com.ben-allred.clj-app-simulator.services.content :as content])
  (:import [java.util Date]))

(defn ^:private prepare [request]
  (content/prepare request #{:content-type :accept} (get-in request [:headers :accept])))

(defn ^:private clean [request]
  (-> request
      (select-keys [:body :query-params :route-params :headers])
      (update :headers (partial maps/map-keys keyword))
      (update :query-params (partial maps/map-keys keyword))
      (assoc :timestamp (Date.))
      (prepare)))

(def init
  (partial conj [:simulators/init]))

(def receive
  (comp (partial conj [:simulators/receive]) clean))

(def reset [:simulators/reset])

(def reset-requests [:http/reset-requests])

(def reset-response [:http/reset-response])

(def change
  (partial conj [:http/change]))
