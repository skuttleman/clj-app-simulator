(ns com.ben-allred.app-simulator.utils.simulators)

(defn config->section [config]
  (when-let [method (keyword (:method config))]
    (or (namespace method)
        (name method))))
