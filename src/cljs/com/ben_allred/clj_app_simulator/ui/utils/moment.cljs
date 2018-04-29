(ns com.ben-allred.clj-app-simulator.ui.utils.moment
  (:require [cljsjs.moment]))

(defn ->moment [value]
  (js/moment value))

(defn from-now [mo]
  (.fromNow mo))

(defn format [mo]
  (.format mo))