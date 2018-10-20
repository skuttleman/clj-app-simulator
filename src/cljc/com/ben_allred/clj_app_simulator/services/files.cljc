(ns com.ben-allred.clj-app-simulator.services.files
  (:require #?(:clj [clojure.core.async :as async])
                    [#?(:clj clj-http.client :cljs cljs-http.client) :as client]
                    [com.ben-allred.clj-app-simulator.services.http :as http]
                    [com.ben-allred.clj-app-simulator.utils.colls :as colls]
                    [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(defn ^:private with-files [request method files mime-type]
  (let [param (if (= method :post) "files" "file")]
    #?(:clj  (assoc request
                    :async? true
                    :multipart (map #(cond-> {:part-name param :name (.getName %) :content %}
                                       mime-type (assoc :mime-type mime-type))
                                    files))
       :cljs (assoc request :multipart-params (map (colls/onto [param]) files)))))

(defn ^:private request* [request method url]
  (let [do-request (if (= method :post) client/post client/put)]
    #?(:clj  (let [chan (async/chan)]
               (do-request url
                           request
                           (partial async/put! chan)
                           (partial async/put! chan))
               chan)
       :cljs (do-request url request))))

(defn upload
  ([url method files]
   (upload url method files "application/transit"))
  ([url method files content-type]
   (upload url method files content-type nil))
  ([url method files content-type mime-type]
   (-> {:headers {"accept" content-type}}
       (with-files method files mime-type)
       (request* method url)
       (http/request*))))
