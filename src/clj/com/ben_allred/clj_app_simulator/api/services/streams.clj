(ns com.ben-allred.clj-app-simulator.api.services.streams
  (:require [com.ben-allred.clj-app-simulator.utils.logging :as log])
  (:import (org.apache.commons.io FileUtils)
           (java.io File)))


(defn open-input-stream [file]
  (when (instance? File file)
    (FileUtils/openInputStream file)))
