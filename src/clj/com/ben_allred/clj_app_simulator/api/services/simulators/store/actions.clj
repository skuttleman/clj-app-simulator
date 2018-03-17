(ns com.ben-allred.clj-app-simulator.api.services.simulators.store.actions
    (:import [java.util Date]))

(defn ^:private clean [request]
    (-> request
        (select-keys [:body :query-params :params :headers])
        (assoc :timestamp (Date.))))

(def init
    (partial conj [:simulators/init]))

(def receive
    (comp (partial conj [:simulators/receive]) clean))

(def reset [:simulators/reset])

(def reset-requests [:http/reset-requests])

(def reset-response [:http/reset-response])

(def change
    (partial conj [:http/change]))
