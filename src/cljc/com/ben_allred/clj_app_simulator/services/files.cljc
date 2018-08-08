(ns com.ben-allred.clj-app-simulator.services.files
  (:require [#?(:clj clj-http.client :cljs cljs-http.client) :as client]
            [#?(:clj clojure.core.async :cljs cljs.core.async) :as async]
            [com.ben-allred.clj-app-simulator.services.http :as http]
            [com.ben-allred.clj-app-simulator.utils.fns :as fns :include-macros true]))

(defn ^:private with-files [request files mime-type]
  #?(:clj  (assoc request
                  :async? true
                  :multipart (map #(cond-> {:part-name "files" :name (.getName %) :content %}
                                     mime-type (assoc :mime-type mime-type))
                                  files))
     :cljs (assoc request :multipart-params (map (partial conj ["files"]) files))))

(defn ^:private request* [request url]
  #?(:clj  (let [chan (async/chan)]
             (client/post url
                          request
                          (fn [response] (async/put! chan response))
                          (fn [error] (async/put! chan error)))
             chan)
     :cljs (client/post url request)))

(defn upload
  ([url files]
   (upload url files "application/transit"))
  ([url files content-type]
   (upload url files content-type nil))
  ([url files content-type mime-type]
   (-> {:headers {"accept" content-type}}
       (with-files files mime-type)
       (request* url)
       (http/request*))))
