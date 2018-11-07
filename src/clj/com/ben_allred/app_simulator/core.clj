(ns com.ben-allred.app-simulator.core
  (:refer-clojure :exclude [reset!])
  (:require
    [com.ben-allred.app-simulator.api.server :as server]
    [com.ben-allred.app-simulator.api.simulators :as api.sim]
    [com.ben-allred.app-simulator.api.resources :as api.res])
  (:import
    (java.util UUID)))

(defn start
  "Starts a web server on the specified port that supplies the web API. Returns a 0-arity function that stops
  the server."
  ([]
   (start 3000))
  ([^Integer port]
   (first (server/-main :port port))))

(defn list-simulators
  "Returns a list of simulators in their current state."
  []
  (api.sim/list))

(defn create-simulator
  "Creates a simulator from a valid configuration. Will throw if the configuration is invalid."
  [config]
  (api.sim/create config))

(defn init-simulators
  "Wipes out any existing simulators and creates a new batch of simulators from the supplied configs. Throws if
  any configs are invalid."
  [configs]
  (api.sim/init configs))

(defn delete-simulators!
  "Deletes the simulator specified by id or all simulators if no id is supplied."
  ([]
   (api.sim/delete!))
  ([^UUID simulator-id]
    (api.sim/delete! simulator-id)))

(defn details
  "Gets the details for a simulator by id. Returns nil if not found"
  [^UUID simulator-id]
  (api.sim/details simulator-id))

(defn list-resources
  "Lists any stored resources (files) that can be used as the response body for file simulators."
  []
  (api.res/list))

(defn save-resources
  "Saves one or more resources to be used as the response body for file simulators."
  [resources]
  (api.res/save resources))

(defn put-resource
  "Saves a resource with a specific id, possibly replacing an existing resource."
  [^UUID resource-id resource]
  (api.res/put resource-id resource))

(defn delete-resources!
  "Deletes the resource specified by id or all resources if no id is supplied."
  ([]
   (api.res/delete!))
  ([^UUID resource-id]
   (api.res/delete! resource-id)))

(defn reset!
  "Resets a simulator state back to its initial state. Resets all simulators if no id is supplied."
  ([]
   (api.sim/reset-all!))
  ([^UUID simulator-id]
   (api.sim/reset! simulator-id {:action :simulators/reset})))

(defn partially-reset!
  "Does a partial reset of a specific simulator as identified by type."
  [^UUID simulator-id type]
  (api.sim/reset! simulator-id {:action :simulators/reset
                                :type   type}))

(defn change!
  "Updates a simulator and merges in the details of the new config."
  [^UUID simulator-id config]
  (api.sim/reset! simulator-id {:action :simulators/change
                                :config config}))

(defn disconnect!
  "Disconnects a websocket specified by websocket-id from a specific simulator. Disconnects all websockets from
  that simulator if no websocket-id is specified."
  ([^UUID simulator-id]
   (api.sim/reset! simulator-id {:action :simulators.ws/disconnect}))
  ([^UUID simulator-id ^UUID websocket-id]
   (api.sim/reset! simulator-id {:action    :simulators.ws/disconnect
                                 :socket-id websocket-id})))

(defn broadcast!
  "Sends a message to all websockets connected to a specific simulator."
  [^UUID simulator-id ^String message]
  (api.sim/send! simulator-id message))

(defn send!
  "Sends a message to a websocket specified by websocket-id connected to a specific simulator."
  [^UUID simulator-id ^UUID websocket-id ^String message]
  (api.sim/send! simulator-id websocket-id message))
