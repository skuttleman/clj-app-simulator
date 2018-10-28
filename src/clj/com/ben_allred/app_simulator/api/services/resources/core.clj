(ns com.ben-allred.app-simulator.api.services.resources.core
  (:refer-clojure :exclude [get])
  (:require
    [clojure.set :as set]
    [com.ben-allred.app-simulator.api.services.activity :as activity]
    [com.ben-allred.app-simulator.api.services.streams :as streams]
    [com.ben-allred.app-simulator.utils.fns :as fns]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [com.ben-allred.app-simulator.utils.maps :as maps]
    [com.ben-allred.app-simulator.utils.uuids :as uuids])
  (:import
    (java.util Date)))

(defonce ^:private uploads (atom {}))

(defn ^:private file->data [[id file]]
  (-> file
      (select-keys [:filename :content-type :timestamp])
      (assoc :id id)))

(defn ^:private upload* [env id file]
  (when (streams/file? (:tempfile file))
    (let [file' (-> file
                    (set/rename-keys {:tempfile :file})
                    (assoc :timestamp (Date.)))
          result (file->data [id file'])]
      (swap! uploads update-in [env id] (comp (constantly file') streams/delete :file))
      result)))

(defn ^:private add! [env key idfn]
  (comp (map #(upload* env (idfn) %))
        (filter some?)
        (fns/each! (comp (partial activity/publish env key)
                         (maps/onto :resource)))))

(defn upload!
  ([env resource-id file]
   (->> [file]
        (fns/transv (add! env :resources/put (constantly (uuids/->uuid resource-id))))
        (first)))
  ([env files]
   (fns/transv (add! env :resources/put uuids/random) files)))

(defn clear! [env]
  (->> (clojure.core/get @uploads env)
       (vals)
       (map (comp streams/delete :file))
       (dorun))
  (swap! uploads dissoc env)
  (activity/publish env :resources/clear nil))

(defn remove! [env id]
  (let [id (uuids/->uuid id)]
    (when-let [resource (get-in @uploads [env id])]
      (streams/delete (:file resource))
      (swap! uploads update env dissoc id)
      (activity/publish env :resources/remove {:resource (file->data [id resource])}))))

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
