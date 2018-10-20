(ns com.ben-allred.clj-app-simulator.core
  (:require
    [com.ben-allred.clj-app-simulator.api.server :as server]))

(defn start [& [port]]
  (first (server/-main :port (or port 3000))))
