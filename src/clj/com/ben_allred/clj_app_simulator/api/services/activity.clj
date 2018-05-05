(ns com.ben-allred.clj-app-simulator.api.services.activity
  (:require [com.ben-allred.clj-app-simulator.utils.json :as json]
            [immutant.web.async :as web.async]
            [com.ben-allred.clj-app-simulator.utils.transit :as transit]
            [com.ben-allred.clj-app-simulator.services.emitter :as emitter]
            [clojure.core.async :as async]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(def ^:private emitter (emitter/new))

(def ^:private accept->stringify
  (comp #(or % json/stringify)
        {"application/edn"     pr-str
         "application/transit" transit/stringify}))

(defn sub [{:keys [query-params headers] :as request}]
  (when (or (:websocket? request) (= "websocket" (get headers "upgrade")))
    (let [stringify (accept->stringify (get query-params "accept"))
          chan (async/chan 100)]
      (web.async/as-channel
        request
        {:on-open  (fn [websocket]
                     (emitter/on emitter chan)
                     (async/go-loop [data (async/<! chan)]
                       (when-let [[event data] data]
                         (web.async/send! websocket (stringify {:event event :data data}))
                         (recur (async/<! chan)))))
         :on-close (fn [_ _]
                     (async/close! chan))}))))

(defn publish [event data]
  (emitter/publish emitter event data))
