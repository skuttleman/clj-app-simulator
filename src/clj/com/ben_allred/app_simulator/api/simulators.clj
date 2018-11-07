(ns com.ben-allred.app-simulator.api.simulators
  (:refer-clojure :exclude [list reset!])
  (:require
    [com.ben-allred.app-simulator.api.core :refer [de-http]]
    [com.ben-allred.app-simulator.api.services.simulators.core :as simulators]
    [com.ben-allred.app-simulator.api.services.simulators.routes :as routes.sim]
    [com.ben-allred.app-simulator.api.services.simulators.simulators :as sims]
    [com.ben-allred.app-simulator.services.env :as env]
    [com.ben-allred.app-simulator.api.services.simulators.common :as common]))

(defn ^:private find-by-id [simulator-id]
  (sims/get (env/env*) simulator-id))

(defn list []
  (de-http
    (simulators/details (env/env*))))

(defn create [config]
  (de-http
    (simulators/add (env/env*) config)))

(defn init [configs]
  (de-http
    (simulators/set! (env/env*) configs)))

(defn delete!
  ([]
   (init [])
   nil)
  ([simulator-id]
   (de-http
     (when-let [simulator (some->> simulator-id
                                   (find-by-id))]
       (let [env (env/env*)]
         ((routes.sim/delete-sim env simulator (partial sims/remove! env)) nil))
       nil))))

(defn details [simulator-id]
  (de-http
    (some->> simulator-id
             (find-by-id)
             (common/details))))

(defn reset-all! []
  (de-http
    (simulators/reset-all! (env/env*))
    nil))

(defn reset! [simulator-id body]
  (de-http
    (when-let [sim (some-> simulator-id
                           (find-by-id))]
      ((routes.sim/patch (env/env*) sim) {:body body})
      nil)))

(defn send!
  ([simulator-id message]
   (de-http
     (some-> simulator-id
             (find-by-id)
             (common/send! message))))
  ([simulator-id ws-id message]
   (de-http
     (some-> simulator-id
             (find-by-id)
             (common/send! ws-id message)))))
