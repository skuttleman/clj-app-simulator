(ns com.ben-allred.clj-app-simulator.api.services.activity
  (:require
    [clojure.core.async :as async]
    [com.ben-allred.clj-app-simulator.services.emitter :as emitter]
    [com.ben-allred.clj-app-simulator.utils.json :as json]
    [com.ben-allred.clj-app-simulator.utils.logging :as log]
    [com.ben-allred.clj-app-simulator.utils.transit :as transit]
    [immutant.web.async :as web.async]))

(def ^:private emitter (emitter/new))

(def ^:private accept->stringify
  (comp #(or % json/stringify)
        {"application/edn"     pr-str
         "application/transit" transit/stringify}))

(defn sub [env {:keys [query-params websocket?] :as request}]
  (when websocket?
    (let [stringify (accept->stringify (get query-params "accept"))
          chan (async/chan 100)]
      (web.async/as-channel
        request
        {:on-open  (fn [websocket]
                     (emitter/on emitter env chan)
                     (async/go-loop [data (async/<! chan)]
                       (when-let [[event data] data]
                         (web.async/send! websocket (stringify {:event event :data data}))
                         (recur (async/<! chan)))))
         :on-close (fn [_ _]
                     (async/close! chan))}))))

(defn publish [env event data]
  (emitter/publish emitter env event data))
