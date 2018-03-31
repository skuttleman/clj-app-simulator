(ns integration.utils.fixtures
  (:require [com.ben-allred.clj-app-simulator.api.server :as server]
            [integration.config :as cfg]))

(defn run-server [test]
  (let [[stop-server! stop-repl!] (server/-main :port cfg/port)]
    (test)
    (stop-server!)
    (when stop-repl!
      (stop-repl!))))
