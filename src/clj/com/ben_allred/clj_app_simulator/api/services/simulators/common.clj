(ns com.ben-allred.clj-app-simulator.api.services.simulators.common
  (:refer-clojure :exclude [send])
  (:require [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(defprotocol ISimulator
  (start [this])
  (stop [this])
  (receive [this request])
  (requests [this])
  (details [this])
  (reset [this])
  (routes [this]))

(defprotocol IHTTPSimulator
  (reset-requests [this])
  (reset-response [this])
  (change [this config]))

(defprotocol IWSSimulator
  (connect [this request])
  (reset-messages [this])
  (disconnect [this] [this socket-id])
  (send [this message] [this socket-id message]))
