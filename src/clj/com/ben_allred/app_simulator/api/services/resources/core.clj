(ns com.ben-allred.app-simulator.api.services.resources.core
  (:refer-clojure :exclude [get])
  (:require
    [clojure.set :as set]
    [com.ben-allred.app-simulator.api.services.activity :as activity]
    [com.ben-allred.app-simulator.api.services.streams :as streams]
    [com.ben-allred.app-simulator.utils.fns :as fns]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [com.ben-allred.app-simulator.utils.uuids :as uuids]
    [com.ben-allred.app-simulator.api.utils.specs :as specs]
    [com.ben-allred.app-simulator.api.utils.respond :as respond])
  (:import
    (java.util Date)))

(defonce ^:private uploads (atom {}))

(defn ^:private file->data [file]
  (select-keys file #{:id :filename :content-type :timestamp}))

(defn ^:private api->file [id-fn]
  (fn [file]
    (-> file
        (set/rename-keys {:tempfile :file})
        (assoc :timestamp (Date.) :id (id-fn)))))

(defn ^:private validate! [param files]
  (when (empty? files)
    (respond/abort! :resources/empty {:param param}))
  (when-not (every? (partial specs/valid? :resource/upload) files)
    (respond/abort! :resources/failed-spec
                    {:files    files
                     :param    param
                     :problems (map (partial specs/explain :resource/upload) files)}))
  files)

(defn ^:private upload* [env id-fn files]
  (->> files
       (map (api->file id-fn))
       (fns/each! #(swap! uploads update-in [env (:id %)] (comp (constantly %) streams/delete :file)))
       (map file->data)
       (fns/each! #(activity/publish env :resources/put {:resource %}))
       (doall)))

(defn upload!
  ([env resource-id file]
   (->> [file]
        (validate! "file")
        (upload* env (constantly (uuids/->uuid resource-id)))
        (first)))
  ([env files]
   (->> files
        (validate! "files")
        (upload* env uuids/random))))

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
      (activity/publish env :resources/remove {:resource (file->data resource)}))))

(defn list-files [env]
  (->> @uploads
       (env)
       (vals)
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
