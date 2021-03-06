(ns com.ben-allred.app-simulator.api.services.simulators.common
  (:refer-clojure :exclude [reset! type]))

(defprotocol IRun
  (start! [this])
  (stop! [this]))

(defprotocol IIdentify
  (details [this])
  (method [this])
  (path [this])
  (type [this]))

(defprotocol IReceive
  (receive! [this request])
  (received [this]))

(defprotocol IReset
  (reset! [this] [this config]))

(defprotocol IRoute
  (routes [this]))

(defprotocol IPartiallyReset
  (partially-reset! [this type]))

(defprotocol ICommunicate
  (connect! [this connection])
  (disconnect! [this] [this id])
  (send! [this message] [this id message]))

(extend-type Object
  IRun
  (start! [this]
    nil)
  (stop! [this]
    nil)

  IIdentify
  (details [this]
    nil)
  (method [this]
    nil)
  (path [this]
    nil)
  (type [this]
    nil)

  IReceive
  (receive! [this request]
    nil)
  (received [this]
    nil)

  IReset
  (reset!
    ([this]
     nil)
    ([this config]
     nil))

  IRoute
  (routes [this]
    nil)

  IPartiallyReset
  (partially-reset [this type]
    nil)

  ICommunicate
  (connect! [this connection]
    nil)
  (disconnect!
    ([this]
     nil)
    ([this id]
     nil))
  (send!
    ([this message]
     nil)
    ([this id message]
     nil)))
