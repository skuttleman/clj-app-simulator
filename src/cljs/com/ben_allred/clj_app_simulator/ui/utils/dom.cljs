(ns com.ben-allred.clj-app-simulator.ui.utils.dom)

(defn stop-propagation [event]
    (.stopPropagation event))
