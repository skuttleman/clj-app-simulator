(ns com.ben-allred.app-simulator.core
  (:require
    [com.ben-allred.app-simulator.api.server :as server]))

(defn start [& [port]]
  "Starts the ring web server on the specified port and returns it."
  (first (server/-main :port (or port 3000))))
