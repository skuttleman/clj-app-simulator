(ns com.ben-allred.clj-app-simulator.api.services.resources.core
  (:refer-clojure :exclude [get])
  (:require [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.utils.uuids :as uuids]
            [clojure.set :as set]
            [com.ben-allred.clj-app-simulator.api.services.activity :as activity])
  (:import (java.util Date)))

(defonce ^:private uploads (atom {}))

(defn ^:private file->data [[id file]]
  (-> file
      (select-keys [:filename :content-type :timestamp])
      (assoc :id id)))

(defn ^:private upload* [id file]
  (let [file' (-> file
                  (set/rename-keys {:tempfile :file})
                  (assoc :timestamp (Date.)))
        result (file->data [id file'])]
    (swap! uploads assoc id file')
    result))

(defn upload!
  ([resource-id file]
   (let [result (upload* (uuids/->uuid resource-id) file)]
     (activity/publish :resources/put result)
     result))
  ([files]
   (let [result (map (fn [file] (upload* (uuids/random) file)) files)]
     (doseq [file result]
       (activity/publish :resources/add file))
     result)))

(defn clear! []
  (reset! uploads {})
  (activity/publish :resources/clear nil))

(defn remove! [id]
  (let [id (uuids/->uuid id)]
    (when-let [resource (clojure.core/get @uploads id)]
      (swap! uploads dissoc id)
      (activity/publish :resources/remove (file->data [id resource])))))

(defn list-files []
  (->> @uploads
       (map file->data)
       (sort-by :timestamp)))

(defn get [id]
  (clojure.core/get @uploads (uuids/->uuid id)))

(defn has-upload? [id]
  (boolean (get id)))
