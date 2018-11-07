(ns com.ben-allred.app-simulator.api.resources
  (:refer-clojure :exclude [list])
  (:require
    [com.ben-allred.app-simulator.api.core :refer [de-http]]
    [com.ben-allred.app-simulator.api.services.resources.core :as resources]
    [com.ben-allred.app-simulator.services.env :as env]))


(defn list []
  (de-http
    (resources/list-files (env/env*))))


(defn save [files]
  (de-http
    (resources/upload! (env/env*) files)))

(defn put [resource-id file]
  (de-http
    (resources/upload! (env/env*) resource-id file)))

(defn delete!
  ([]
   (de-http
     (resources/clear! (env/env*)))
    nil)
  ([resource-id]
   (de-http
     (resources/remove! (env/env*) resource-id))
    nil))
