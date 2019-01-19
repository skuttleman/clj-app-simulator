(ns com.ben-allred.app-simulator.ui.services.store.middleware
  (:require
    [com.ben-allred.app-simulator.utils.colls :as colls]))

(defn sims->sim [_]
  (fn [next]
    (fn [[type data :as action]]
      (if (= type :simulators.fetch-all/succeed)
        (->> data (:simulators)
             (sequence (comp (map (partial assoc {} :simulator))
                             (map (partial conj [:simulators.fetch-one/succeed]))
                             (colls/prepend [:simulators/clear])
                             (map next)))
             (dorun))
        (next action)))))
