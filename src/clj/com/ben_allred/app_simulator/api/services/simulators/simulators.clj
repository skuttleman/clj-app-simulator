(ns com.ben-allred.app-simulator.api.services.simulators.simulators
  (:refer-clojure :exclude [get])
  (:require
    [com.ben-allred.app-simulator.api.services.simulators.common :as common]
    [com.ben-allred.app-simulator.utils.sets :as sets]
    [com.ben-allred.app-simulator.utils.logging :as log]))

(defonce ^:private sims (atom {}))

(defn ^:private simulators* [env]
  (-> @sims
      (clojure.core/get env)
      (seq)))

(defn clear! [env]
  (-> @sims
      (clojure.core/get env)
      (->> (run! common/stop!)))
  (swap! sims dissoc env))

(defn add! [env simulator]
  (when-not (contains? (clojure.core/get @sims env) simulator)
    (swap! sims update env (fnil conj sets/ordered) simulator)
    (common/start! simulator)
    simulator))

(defn simulators [env]
  (->> (simulators* env)
       (sort-by common/type (comp (partial * -1) compare))))

(defn get [env simulator-id]
  (->> (simulators* env)
       (sequence (filter (comp #{simulator-id} :id common/details)))
       (first)))

(defn remove! [env sim]
  (when (contains? (clojure.core/get @sims env) sim)
    (common/stop! sim)
    (swap! sims update env disj sim)))
