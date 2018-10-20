(ns com.ben-allred.clj-app-simulator.api.services.simulators.store.actions
  (:require
    [com.ben-allred.clj-app-simulator.services.content :as content]
    [com.ben-allred.clj-app-simulator.utils.logging :as log]
    [com.ben-allred.clj-app-simulator.utils.maps :as maps]
    [immutant.web.async :as web.async])
  (:import
    (java.util Date)))

(defn ^:private prepare [request]
  (content/prepare request #{:content-type :accept} (get-in request [:headers :accept])))

(defn ^:private clean [request]
  (-> request
      (select-keys [:body :query-params :route-params :headers :socket-id :message-id])
      (update :headers (partial maps/map-keys keyword))
      (update :query-params (partial maps/map-keys keyword))
      (prepare)
      (assoc :timestamp (Date.))))

(defn ^:private get-sockets [state]
  (remove nil? (vals (:sockets state))))

(defn ^:private get-socket [state socket-id]
  (get-in state [:sockets socket-id]))

(defn find-socket-id [state ws]
  (->> (:sockets state)
       (filter (comp #{ws} second))
       (ffirst)))

;; simulators

(defn init [config]
  [:simulators/init config])

(defn receive [request]
  [:simulators/receive (clean request)])

(def reset [:simulators/reset])

(defn change [config]
  [:simulators/change config])

;; http

(def reset-requests [:simulators.http/reset-requests])

(def reset-response [:simulators.http/reset-response])

;; ws

(def reset-messages [:simulators.ws/reset-messages])

(defn connect [socket-id ws]
  [:simulators.ws/connect socket-id ws])

(defn remove-socket [socket-id]
  [:simulators.ws/remove socket-id])

(defn send-one [socket-id message]
  (fn [[_ get-state]]
    (some-> (get-state)
            (get-socket socket-id)
            (web.async/send! message))))

(defn send-all [message]
  (fn [[_ get-state]]
    (->> (get-state)
         (get-sockets)
         (map #(web.async/send! % message))
         (dorun))))

(defn disconnect [socket-id]
  (fn [[_ get-state]]
    (some-> (get-state)
            (get-socket socket-id)
            (web.async/close))))

(defn disconnect-all [[_ get-state]]
  (->> (get-state)
       (get-sockets)
       (map web.async/close)
       (dorun)))
