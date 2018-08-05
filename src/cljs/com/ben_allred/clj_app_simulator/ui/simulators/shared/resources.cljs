(ns com.ben-allred.clj-app-simulator.ui.simulators.shared.resources
  (:require [com.ben-allred.clj-app-simulator.services.http :as http]
            [com.ben-allred.clj-app-simulator.utils.strings :as strings]))

(def statuses
  (->> http/kw->status
       (map (juxt second #(str (strings/titlize (name (first %)) " ") " [" (second %) "]")))
       (sort-by first)))
