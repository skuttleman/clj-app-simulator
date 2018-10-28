(ns user
  (:require
    [com.ben-allred.app-simulator.services.env :as env]
    [figwheel-sidecar.repl-api :as f]))

(defn cljs-repl []
  (f/cljs-repl))

(defn update-env [key val]
  (alter-var-root #'env/get assoc key val))
