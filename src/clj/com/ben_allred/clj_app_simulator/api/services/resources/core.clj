(ns com.ben-allred.clj-app-simulator.api.services.resources.core
  (:refer-clojure :exclude [get])
  (:require [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.utils.uuids :as uuids]
            [clojure.set :as set]
            [com.ben-allred.clj-app-simulator.api.services.activity :as activity])
  (:import (java.util Date)))

(def ^:private uploads (atom {}))

(defn ^:private file->data [[id file]]
  (-> file
      (select-keys [:filename :content-type :timestamp])
      (assoc :id id)))

(defn upload! [files]
  (let [id'ed-files (->> files
                         (map (fn [file]
                                [(uuids/random)
                                 (-> file
                                     (set/rename-keys {:tempfile :file})
                                     (assoc :timestamp (Date.)))])))
        result (map file->data id'ed-files)]
    (swap! uploads into id'ed-files)
    (doseq [file result]
      (activity/publish :files.upload/receive file))
    result))

(defn clear! []
  (reset! uploads {})
  (activity/publish :files/clear nil))

(defn remove! [id]
  (when-let [resource (clojure.core/get @uploads id)]
    (swap! uploads dissoc id)
    (activity/publish :files/remove (file->data [id resource]))))

(defn list-files []
  (->> @uploads
       (map file->data)
       (sort-by :timestamp)))

(defn get [id]
  (clojure.core/get @uploads id))

(defn has-upload? [id]
  (boolean (get id)))
