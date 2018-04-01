(ns com.ben-allred.clj-app-simulator.services.http
  (:refer-clojure :exclude [get])
  (:require #?(:clj  [clojure.core.async :as async]
               :cljs [cljs.core.async :as async])
                     [kvlt.chan :as kvlt]
                     [com.ben-allred.clj-app-simulator.services.content :as content]
                     [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(def ^:private content-type
  #?(:clj  "application/json"
     :cljs "application/transit"))

(def ^:private header-keys #{:content-type :accept})

(defn ^:private content-type-header [{:keys [headers]}]
  (clojure.core/get headers "content-type" (:content-type headers)))

(def status->kw
  {200 :ok
   201 :created
   202 :accepted
   204 :no-content
   400 :bad-request
   401 :unauthorized
   403 :forbidden
   404 :not-found
   409 :conflict
   424 :failed-dependency
   500 :internal-server-error
   501 :not-implemented
   503 :service-unavailable
   504 :gateway-timeout})

(def kw->status
  (into {} (map (comp vec reverse)) status->kw))

(defn ^:private check-status [lower upper response]
  (let [status (if (vector? response)
                 (kw->status (clojure.core/get response 2))
                 (:status response))]
    (<= lower status upper)))

(def success?
  (partial check-status 200 299))

(def client-error?
  (partial check-status 400 499))

(def server-error?
  (partial check-status 500 599))

(defn request* [method url request]
  (async/go
    (let [content-type (get-in request [:headers :content-type])
          ch-response (async/<! (-> request
                                    (assoc :method method :url url)
                                    (content/prepare header-keys content-type)
                                    (update :headers merge (:headers request))
                                    (update-in [:headers :content-type] #(or % "application/transit"))
                                    (update-in [:headers :accept] #(or % "application/transit"))
                                    (kvlt/request!)))
          {:keys [status] :as response} (if-let [data (ex-data ch-response)]
                                          data
                                          ch-response)
          body (-> response
                   (content/parse (content-type-header response))
                   (:body))
          status (status->kw status status)]
      (if (success? response)
        [:success body status response]
        [:error body status response]))))

(defn get [url & [request]]
  (request* :get url request))

(defn post [url request]
  (request* :post url request))

(defn patch [url request]
  (request* :patch url request))

(defn put [url request]
  (request* :put url request))

(defn delete [url request]
  (request* :delete url request))
