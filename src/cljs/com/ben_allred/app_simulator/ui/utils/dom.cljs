(ns com.ben-allred.app-simulator.ui.utils.dom
  (:require
    [clojure.set :as set]))

(def ^:private key->code
  {:key-codes/esc 27
   :key-codes/enter 13})

(def ^:private code->key
  (set/map-invert key->code))

(defn stop-propagation [event]
  (.stopPropagation event))

(defn prevent-default [event]
  (.preventDefault event))

(defn target-value [event]
  (some-> event
          (.-target)
          (.-value)))

(defn query-one [selector]
  (.querySelector js/document selector))

(defn click [node]
  (.click node))

(defn focus [node]
  (.focus node)
  (when (.-setSelectionRange node)
    (let [length (-> node (.-value) (.-length))]
      (.setSelectionRange node length length))))

(defn event->key [e]
  (-> e
      (.-keyCode)
      (code->key)))
