(ns integration.utils.chans
  (:require [clojure.core.async :as async]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(defn flush! [chan]
  (let [alt (async/go
              (Thread/sleep 250)
              ::ignore)]
    (first (async/alts!! [alt chan]))))
