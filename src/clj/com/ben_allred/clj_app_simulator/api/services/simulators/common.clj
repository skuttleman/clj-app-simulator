(ns com.ben-allred.clj-app-simulator.api.services.simulators.common
  (:refer-clojure :exclude [send]))

(defprotocol ISimulator
  (start [this])
  (stop [this])
  (receive [this request])
  (requests [this])
  (details [this])
  (reset [this])
  (routes [this delete]))

(defprotocol IHTTPSimulator
  (reset-requests [this])
  (reset-response [this])
  (change [this config]))

(defprotocol IWSSimulator
  (connect [this request])
  (disconnect [this] [this socket-id])
  (send [this message] [this socket-id message]))
