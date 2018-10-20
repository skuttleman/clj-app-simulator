(ns com.ben-allred.clj-app-simulator.templates.resources.shared
  (:require
    [com.ben-allred.clj-app-simulator.services.http :as http]
    [com.ben-allred.clj-app-simulator.utils.strings :as strings]))

(defn ^:private status-name [[kw status]]
  (str (strings/titlize (name kw) " ") " [" status "]"))

(def statuses
  (->> http/kw->status
       (map (juxt second status-name))
       (sort-by first)))
