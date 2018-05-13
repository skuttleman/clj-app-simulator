(ns integration.utils.fixtures
  (:require [com.ben-allred.clj-app-simulator.core :as sim-core]
            [integration.config :as cfg]))

(defn run-server [test]
  (Thread/sleep 50)
  (let [stop-server! (sim-core/start cfg/port)]
    (test)
    (stop-server!)))
