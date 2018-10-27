(ns integration.utils.chans
  (:require
    [clojure.core.async :as async]
    [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(defn flush!
  ([chan]
   (flush! chan 100))
  ([chan ms]
   (async/go
     (async/<! (async/timeout ms))
     (async/>! chan ::done))
   (async/<!!
     (async/go-loop [v (async/<! chan)]
       (when (not= v ::done)
         (recur (async/<! chan)))))
   nil))

(defn <⏰!!
  ([chan]
   (<⏰!! chan 100))
  ([chan ms]
   (let [[value] (async/alts!! [chan (async/go
                                       (async/<! (async/timeout ms))
                                       ::timed-out)])]
     (when (= ::timed-out value)
       (throw (ex-info "The channel did not produce a value within the expected time." {:chan chan :ms ms})))
     value)))
