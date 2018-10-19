(ns com.ben-allred.clj-app-simulator.api.services.simulators.core
  (:refer-clojure :exclude [set!])
  (:require [com.ben-allred.clj-app-simulator.api.services.simulators.http :as http.sim]
            [com.ben-allred.clj-app-simulator.api.services.simulators.ws :as ws.sim]
            [com.ben-allred.clj-app-simulator.api.services.simulators.file :as file.sim]
            [com.ben-allred.clj-app-simulator.api.services.simulators.simulators :as sims]
            [compojure.core :as c]
            [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
            [com.ben-allred.clj-app-simulator.api.services.activity :as activity]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.utils.uuids :as uuids]))

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
  (when-let [simulator (config->?simulator env config)]
    (sims/add! env simulator)))

(defn details [env]
  (->> (simulator-configs env common/details)
       (assoc {} :simulators)
       (conj [:ok])))

(defn add [env config]
  (if-let [simulator (make-simulator! env config)]
    (let [sim (common/details simulator)]
      (activity/publish env :simulators/add sim)
      [:created {:simulator sim}])
    [:bad-request {:message "error creating simulator"}]))

(defn set! [env configs]
  (let [invalid-configs (remove valid? configs)]
    (if (empty? invalid-configs)
      (do
        (sims/clear! env)
        (let [sims (->> configs
                        (map (partial make-simulator! env))
                        (map common/details))]
          (activity/publish env :simulators/init sims)
          [:created {:simulators sims}]))
      [:bad-request {:message "one or more invalid simulators"}])))

(defn reset-all! [env]
  (let [sims (simulator-configs env)]
    (dorun (map common/reset! sims))
    (activity/publish env :simulators/reset-all (map common/details sims)))
  [:no-content])

(defn routes [env]
  (->> (simulator-configs env common/routes)
       (mapcat identity)
       (apply c/routes)))
