(ns user
    (:require [figwheel-sidecar.repl-api :as f]
              [com.ben-allred.clj-app-simulator.services.env :as env]))

(defn cljs-repl []
    (f/cljs-repl))

(defn update-env [key val]
    (alter-var-root #'env/get assoc key val))
