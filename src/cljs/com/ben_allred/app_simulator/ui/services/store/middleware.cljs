(ns com.ben-allred.app-simulator.ui.services.store.middleware
  (:require
    [com.ben-allred.app-simulator.utils.colls :as colls]
    [com.ben-allred.app-simulator.utils.maps :as maps]))

(defn sims->sim [_]
  (fn [next]
    (fn [[type data :as action]]
      (if (= type :simulators.fetch-all/succeed)
        (->> data (:simulators)
             (sequence (comp (map (maps/onto :simulator))
                             (map (colls/onto [:simulators.fetch-one/succeed]))
                             (colls/prepend [:simulators/clear])
                             (map next)))
             (dorun))
        (next action)))))
