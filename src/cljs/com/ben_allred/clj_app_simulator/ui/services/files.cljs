(ns com.ben-allred.clj-app-simulator.ui.services.files
  (:require [cljs-http.client :as client]
            [com.ben-allred.clj-app-simulator.services.http :as http]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(defn upload [url files]
  (-> url
        (client/post {:multipart-params (map (partial conj ["files"]) files)
                      :headers          {"accept" "application/transit"}})
        (http/request*)))
