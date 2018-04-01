(ns integration.utils.fixtures
  (:require [com.ben-allred.clj-app-simulator.core :as sim-core]
            [integration.config :as cfg]))

(defn run-server [test]
  (let [stop-server! (sim-core/start cfg/port)]
    (test)
    (stop-server!)))
