(ns com.ben-allred.app-simulator.api.utils.specs
  (:require
    [clojure.spec.alpha :as s]
    [com.ben-allred.app-simulator.utils.dates :as dates]
    [com.ben-allred.app-simulator.utils.uuids :as uuids]
    [com.ben-allred.app-simulator.api.services.streams :as streams]
    [com.ben-allred.app-simulator.utils.logging :as log])
  (:import (clojure.lang Named)))

;; utils
(defn ^:private conformer [pred]
  (s/conformer #(or (pred %) ::s/invalid)))

(defn ^:private sor [& conformers]
  (fn [v]
    (loop [[conformer & more] conformers]
      (let [value (s/conform conformer v)]
        (if (or (empty? more)
                (not= value ::s/invalid))
          value
          (recur more))))))

(defmulti ^:private http-patch (comp (partial s/conform :simulator.http.change/action) :action))

(defmethod http-patch :simulators/change
  [patch]
  (s/conform (s/keys :req-un [:simulator.http.change/action :simulator.http.change/config])
             patch))

(defmethod http-patch :simulators/reset
  [patch]
  (s/conform (s/keys :req-un [:simulator.http.change/action]
                     :opt-un [:simulator.http.change/type])
             patch))

(defmethod http-patch :default
  [_]
  (do ::s/invalid))

(defmulti ^:private file-patch (comp (partial s/conform :simulator.file.change/action) :action))

(defmethod file-patch :simulators/change
  [patch]
  (s/conform (s/keys :req-un [:simulator.file.change/action :simulator.file.change/config])
             patch))

(defmethod file-patch :simulators/reset
  [patch]
  (s/conform (s/keys :req-un [:simulator.file.change/action]
                     :opt-un [:simulator.file.change/type])
             patch))

(defmethod file-patch :default
  [_]
  (do ::s/invalid))

(defmulti ^:private ws-patch (comp (partial s/conform :simulator.ws.change/action) :action))

(defmethod ws-patch :simulators/change
  [patch]
  (s/conform (s/keys :req-un [:simulator.ws.change/action :simulator.ws.change/config])
             patch))

(defmethod ws-patch :simulators/reset
  [patch]
  (s/conform (s/keys :req-un [:simulator.ws.change/action]
                     :opt-un [:simulator.ws.change/type])
             patch))

(defmethod ws-patch :simulators.ws/disconnect
  [patch]
  (s/conform (s/keys :req-un [:simulator.ws.change/action]
                     :opt-un [:simulator.ws.change/socket-id])
             patch))

(defmethod ws-patch :default
  [_]
  (do ::s/invalid))

(defn conform [spec config]
  (let [conformed (s/conform spec config)]
    (when-not (= ::s/invalid conformed)
      conformed)))

(defn valid? [spec config]
  (try
    (s/valid? spec config)
    (catch Throwable _
      false)))

(defn explain [spec config]
  (s/explain-data spec config))

(defn assert! [spec config]
  (s/assert spec config))

;; types
(s/def :type/file streams/file?)
(s/def :type/inst (conformer dates/->inst))
(s/def :type/named (s/conformer #(cond
                                   (string? %) %
                                   (instance? Named %) (name %)
                                   :else ::s/invalid)))
(s/def :type/named->string (s/map-of :type/named :type/string))
(s/def :type/named->string|string-coll (s/map-of :type/named :type/string|string-coll))
(s/def :type/string string?)
(s/def :type/string|string-coll (s/conformer #(if (or (string? %)
                                                      (and (coll? %) (every? string? %)))
                                                %
                                                ::s/invalid)))
(s/def :type/uri (partial re-matches #"/|(/:?[A-Za-z-_0-9]+)+"))
(s/def :type/uuid (s/conformer #(cond
                                  (uuid? %) %
                                  (uuids/uuid-str? %) (uuids/->uuid %)
                                  :else ::s/invalid)))

;; vals
(s/def :config/delay #(and (integer? %) (<= 0 %)))
(s/def :config/path :type/uri)
(s/def :config/status #(and (integer? %) (<= 200 % 599)))
(s/def :details/simulator (sor :http.details/simulator :file.details/simulator :ws.details/simulator))
(s/def :details/simulators (s/coll-of :details/simulator))
(s/def :entity/id :type/uuid)
(s/def :entity/timestamp :type/inst)
(s/def :file.change/response (s/keys :opt-un [:config/status :file/file :request/headers]))
(s/def :file.details/requests (s/coll-of :file/request))
(s/def :file.details/simulator (s/keys :req-un [:entity/id :simulator.file/config :file.details/requests]))
(s/def :file/file :type/uuid)
(s/def :file/method (conformer (comp #{:file/get :file/post :file/patch :file/put :file/delete} keyword)))
(s/def :file/request (s/keys :req-un [:entity/id :entity/timestamp :request/query-params
                                      :request/route-params :request/headers]
                             :opt-un [:request/body]))
(s/def :file/request-details (s/keys :req-un [:file.details/simulator :file/request]))
(s/def :file/response (s/keys :req-un [:config/status :file/file] :opt-un [:request/headers]))
(s/def :http.change/response (s/keys :opt-un [:config/status :request/body :request/headers]))
(s/def :http.details/requests (s/coll-of :http/request))
(s/def :http.details/simulator (s/keys :req-un [:entity/id :simulator.http/config :http.details/requests]))
(s/def :http/method (conformer (comp #{:http/get :http/post :http/patch :http/put :http/delete} keyword)))
(s/def :http/request :file/request)
(s/def :http/request-details (s/keys :req-un [:http.details/simulator :http/request]))
(s/def :http/response (s/keys :req-un [:config/status] :opt-un [:request/body :request/headers]))
(s/def :request/body (s/nilable :type/string))
(s/def :request/headers :type/named->string|string-coll)
(s/def :request/query-params :type/named->string)
(s/def :request/route-params :type/named->string)
(s/def :resource/content-type :type/string)
(s/def :resource/filename :type/string)
(s/def :resource/resource (s/keys :req-un [:entity/id :resource/filename :entity/timestamp :resource/content-type]))
(s/def :resource.upload/tempfile :type/file)
(s/def :resource.upload/file :type/file)
(s/def :resource.web/upload (s/keys :req-un [:resource.upload/tempfile]
                                    :opt-un [:resource/filename :resource/content-type]))
(s/def :resource.api/upload (s/keys :req-un [:resource.upload/file]
                                    :opt-un [:resource/filename :resource/content-type]))
(s/def :resource/upload (sor :resource.web/upload :resource.api/upload))
(s/def :simulator.change/config (sor :simulator.http.change/config
                                     :simulator.file.change/config
                                     :simulator.ws.change/config))
(s/def :simulator.file.change/action (conformer (comp #{:simulators/reset :simulators/change} keyword)))
(s/def :simulator.file.change/config (s/keys :opt-un [:file.change/response :config/delay]))
(s/def :simulator.file.change/type (s/nilable (conformer (comp #{:file/config :file/requests :file/response} keyword))))
(s/def :simulator.file/config (s/keys :req-un [:file/method :config/path :file/response] :opt-un [:config/delay]))
(s/def :simulator.file/patch (s/conformer file-patch))
(s/def :simulator.http.change/action (conformer (comp #{:simulators/reset :simulators/change} keyword)))
(s/def :simulator.http.change/config (s/keys :opt-un [:http.change/response :config/delay]))
(s/def :simulator.http.change/type (s/nilable (conformer (comp #{:http/config :http/requests :http/response} keyword))))
(s/def :simulator.http/config (s/keys :req-un [:http/method :config/path :http/response] :opt-un [:config/delay]))
(s/def :simulator.http/patch (s/conformer http-patch))
(s/def :simulator.ws.change/action (conformer (comp #{:simulators/reset :simulators/change :simulators.ws/disconnect}
                                                    keyword)))
(s/def :simulator.ws.change/config map?)
(s/def :simulator.ws.change/socket-id (s/nilable :type/uuid))
(s/def :simulator.ws.change/type (s/nilable (conformer (comp #{:ws/config :ws/requests} keyword))))
(s/def :simulator.ws/config (s/keys :req-un [:ws/method :config/path]))
(s/def :simulator.ws/patch (s/conformer ws-patch))
(s/def :simulator.ws/socket-id :type/uuid)
(s/def :simulator.ws/sockets (s/coll-of :type/uuid))
(s/def :simulator/config (sor :http/simulator-config :file/simulator-config :ws/simulator-config))
(s/def :ws.details/requests (s/coll-of :ws/request))
(s/def :ws.details/simulator (s/keys :req-un [:entity/id :simulator.ws/config
                                              :ws.details/requests :simulator.ws/sockets]))
(s/def :ws/method (conformer (comp #{:ws/ws} keyword)))
(s/def :ws/request (s/keys :req-un [:entity/id :entity/timestamp :request/query-params :request/route-params
                                    :request/headers :simulator.ws/socket-id :request/body]))
(s/def :ws/request-details (s/keys :req-un [:ws.details/simulator :ws/request]))

;; specs
(s/def ::details-simulator (s/keys :req-un [:details/simulator]))
(s/def ::details-simulators (s/keys :req-un [:details/simulators]))
(s/def ::request-details (sor :http/request-details :file/request-details :ws/request-details))
(s/def ::resource-item (s/keys :req-un [:resource/resource]))
(s/def ::socket-simulator (s/keys :req-un [:ws.details/simulator]
                                  :opt-un [:simulator.ws/socket-id]))
