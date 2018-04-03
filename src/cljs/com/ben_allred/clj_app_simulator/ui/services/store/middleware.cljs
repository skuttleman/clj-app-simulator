(ns com.ben-allred.clj-app-simulator.ui.services.store.middleware)

(defn sims->sim [_]
  (fn [next]
    (fn [[type data :as action]]
      (if (= type :simulators.fetch-all/succeed)
        (->> data
             (:simulators)
             (map #(do [:simulators.fetch-one/succeed {:simulator %}]))
             (cons [:simulators/clear])
             (map next)
             (dorun))
        (next action)))))
