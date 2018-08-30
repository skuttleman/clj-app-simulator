(ns com.ben-allred.clj-app-simulator.utils.dom)

(defn stop-propagation [event]
  (.stopPropagation event))

(defn prevent-default [event]
  (.preventDefault event))

(defn target-value [event]
  (when-let [target (.-target event)]
    (.-value target)))

(defn query-one [selector]
  #?(:cljs (.querySelector js/document selector)
     :clj nil))

(defn click [node]
  (.click node))
