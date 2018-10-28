(ns integration.utils.fixtures
  (:require
    [com.ben-allred.app-simulator.api.services.resources.core :as resources]
    [com.ben-allred.app-simulator.api.services.simulators.simulators :as sims]
    [com.ben-allred.app-simulator.core :as sim-core]
    [com.ben-allred.app-simulator.services.env :as env]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [integration.config :as cfg]))

(defn run-server [test]
  (with-redefs [env/get (assoc env/get :ring-env :test)]
    (let [stop-server! (sim-core/start cfg/port)]
      (test)
      (stop-server!)
      (sims/clear! :test)
      (resources/clear! :test))))
