(ns com.ben-allred.app-simulator.ui.utils.dom)

(defn stop-propagation [event]
  (.stopPropagation event))

(defn prevent-default [event]
  (.preventDefault event))

(defn target-value [event]
  (when-let [target (.-target event)]
    (.-value target)))

(defn query-one [selector]
  (.querySelector js/document selector))

(defn click [node]
  (.click node))
