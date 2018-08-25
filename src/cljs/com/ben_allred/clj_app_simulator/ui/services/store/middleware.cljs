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

(defn home-welcome? [next]
  (fn [reducer initial-state]
    (let [welcome? (atom true)]
      (next (fn [state action]
              (let [next-state (reducer state action)
                    {up-status :status up-data :data} (:uploads next-state)
                    {sim-status :status sim-data :data} (:simulators next-state)
                    result (and @welcome?
                                (= up-status sim-status :available)
                                (empty? up-data)
                                (empty? sim-data))]
                (when (and @welcome?
                           (or (and (= up-status :available)
                                    (seq up-data))
                               (and (= sim-status :available)
                                    (seq sim-data))))
                  (reset! welcome? false))
                (assoc next-state :home-welcome? result)))
            (assoc initial-state :home-welcome? false)))))

(defn uploads-welcome? [next]
  (fn [reducer initial-state]
    (let [welcome? (atom true)]
      (next (fn [state action]
              (let [next-state (reducer state action)
                    {:keys [status data]} (:uploads next-state)
                    result (and @welcome? (= status :available) (empty? data))]
                (when (and @welcome? (= status :available) (seq data))
                  (reset! welcome? false))
                (assoc next-state :uploads-welcome? result)))
            (assoc initial-state :uploads-welcome? false)))))
