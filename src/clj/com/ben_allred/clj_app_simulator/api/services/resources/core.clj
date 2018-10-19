(ns com.ben-allred.clj-app-simulator.api.services.resources.core
  (:refer-clojure :exclude [get])
  (:require [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.utils.uuids :as uuids]
            [clojure.set :as set]
            [com.ben-allred.clj-app-simulator.api.services.activity :as activity])
  (:import (java.io File)
           (java.util Date)))

(defonce ^:private uploads (atom {}))

(defn ^:private file->data [[id file]]
  (-> file
      (select-keys [:filename :content-type :timestamp])
      (assoc :id id)))

(defn ^:private upload* [env id file]
  (let [file' (-> file
                  (set/rename-keys {:tempfile :file})
                  (assoc :timestamp (Date.)))
        result (file->data [id file'])]
    (swap! uploads update-in [env id] (fn [old-file?]
                                        (when old-file?
                                          (.delete ^File (:file old-file?)))
                                        file'))
    result))

(defn upload!
  ([env resource-id file]
   (let [result (upload* env (uuids/->uuid resource-id) file)]
     (activity/publish env :resources/put result)
     result))
  ([env files]
   (let [result (map (fn [file] (upload* env (uuids/random) file)) files)]
     (doseq [file result]
       (activity/publish env :resources/add file))
     result)))

(defn clear! [env]
  (doseq [[_ {:keys [file]}] (clojure.core/get @uploads env)]
    (.delete ^File file))
  (swap! uploads dissoc env)
  (activity/publish env :resources/clear nil))

(defn remove! [env id]
  (let [id (uuids/->uuid id)]
    (when-let [resource (get-in @uploads [env id])]
      (.delete ^File (:file resource))
      (swap! uploads update env dissoc id)
      (activity/publish env :resources/remove (file->data [id resource])))))

(defn list-files [env]
  (->> @uploads
       (env)
       (map file->data)
       (sort-by :timestamp)))

(defn get [env id]
  (get-in @uploads [env (uuids/->uuid id)]))

(defn has-upload? [id]
  (let [id (uuids/->uuid id)]
    (->> @uploads
         (vals)
         (some #(clojure.core/get % id))
         (boolean))))
