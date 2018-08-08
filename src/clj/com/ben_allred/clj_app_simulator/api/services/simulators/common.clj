(ns com.ben-allred.clj-app-simulator.api.services.simulators.common
  (:refer-clojure :exclude [send])
  (:require [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(defprotocol ISimulator
  (start [this])
  (stop [this])
  (receive [this request])
  (requests [this])
  (reset-requests [this])
  (details [this])
  (identifier [this])
  (change [this config])
  (reset [this])
  (routes [this]))

(defprotocol IHTTPSimulator
  (reset-response [this]))

(defprotocol IWSSimulator
  (connect [this request])
  (disconnect [this] [this socket-id])
  (send [this message] [this socket-id message]))
