(ns integration.utils.chans
  (:require [clojure.core.async :as async]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(defn flush!
  ([chan]
   (flush! chan 100))
  ([chan ms]
   (async/go
     (Thread/sleep ms)
     (async/>! chan ::done))
   (async/<!!
     (async/go-loop [v (async/<! chan)]
       (when (not= v ::done)
         (recur (async/<! chan)))))
   nil))
