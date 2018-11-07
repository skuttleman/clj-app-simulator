(ns integration.utils.ws
  (:require
    [com.ben-allred.app-simulator.services.ws :as ws]
    [integration.config :as cfg]))

(defn connect [path & opts]
  (apply ws/connect (cfg/->url :ws path) opts))

(def send! ws/send!)

(def close! ws/close!)

(def closed? ws/closed?)
