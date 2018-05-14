(ns com.ben-allred.clj-app-simulator.ui.utils.simulators)

(defn config->section [config]
  (when-let [method (keyword (:method config))]
    (or (namespace method)
        (name method))))
