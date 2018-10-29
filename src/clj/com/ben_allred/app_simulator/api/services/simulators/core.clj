(ns com.ben-allred.app-simulator.api.services.simulators.core
  (:refer-clojure :exclude [set!])
  (:require
    [com.ben-allred.app-simulator.api.services.activity :as activity]
    [com.ben-allred.app-simulator.api.services.simulators.common :as common]
    [com.ben-allred.app-simulator.api.services.simulators.file :as file.sim]
    [com.ben-allred.app-simulator.api.services.simulators.http :as http.sim]
    [com.ben-allred.app-simulator.api.services.simulators.simulators :as sims]
    [com.ben-allred.app-simulator.api.services.simulators.ws :as ws.sim]
    [com.ben-allred.app-simulator.api.utils.respond :as respond]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [com.ben-allred.app-simulator.utils.uuids :as uuids]
    [compojure.core :as c]))

(def ^:private validators [#'http.sim/valid?
                           #'ws.sim/valid?
                           #'file.sim/valid?])

(def ^:private sim-fns [#'http.sim/->HttpSimulator
                        #'ws.sim/->WsSimulator
                        #'file.sim/->FileSimulator])

(defn ^:private simulator-configs
  ([env]
   (sims/simulators env))
  ([env f]
   (map f (sims/simulators env))))

(defn valid? [config]
  (some #(% config) validators))

(defn config->?simulator [env config]
  (let [id (uuids/random)]
    (->> sim-fns
         (keep #(% env id config))
         (first))))

(defn make-simulator! [env config]
  (or (some->> config (config->?simulator env) (sims/add! env))
      (respond/abort! :simulators.add/duplicate-sim)))

(defn details [env]
  (->> (simulator-configs env common/details)
       (assoc {} :simulators)
       (conj [:ok])))

(defn add [env config]
  (when-not (valid? config)
    (respond/abort! :simulators.add/failed-spec))
  (let [simulator (common/details (make-simulator! env config))]
    (activity/publish env :simulators/add {:simulator simulator})
    [:created {:simulator simulator}]))

(defn set! [env configs]
  (when (seq (remove valid? configs))
    (respond/abort! :simulators.init/failed-spec))
  (sims/clear! env)
  (let [sims (->> configs
                  (map (comp common/details (partial make-simulator! env)))
                  (assoc {} :simulators))]
    (activity/publish env :simulators/init sims)
    [:created sims]))

(defn reset-all! [env]
  (let [sims (simulator-configs env)]
    (dorun (map common/reset! sims))
    (activity/publish env :simulators/reset-all {:simulators (map common/details sims)}))
  [:no-content])

(defn routes [env]
  (->> (simulator-configs env common/routes)
       (mapcat identity)
       (apply c/routes)))
