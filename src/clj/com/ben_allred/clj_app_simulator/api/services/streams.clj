(ns com.ben-allred.clj-app-simulator.api.services.streams
  (:require
    [com.ben-allred.clj-app-simulator.utils.logging :as log])
  (:import
    (java.io File InputStream)
    (org.apache.commons.io FileUtils)))


(defn open-input-stream [file]
  (when (instance? File file)
    (FileUtils/openInputStream ^File file)))

(defn delete [file]
  (when (instance? File file)
    (.delete ^File file)))

(defn input-stream? [value]
  (instance? InputStream value))
