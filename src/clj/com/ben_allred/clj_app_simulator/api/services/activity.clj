(ns com.ben-allred.clj-app-simulator.api.services.activity
    (:require [org.httpkit.server :as httpkit]
              [com.ben-allred.clj-app-simulator.utils.logging :as log]
              [com.ben-allred.clj-app-simulator.utils.json :as json]
              [com.ben-allred.clj-app-simulator.utils.transit :as transit]
              [com.ben-allred.clj-app-simulator.services.emitter :as emitter]
              [clojure.core.async :as async]))

(def ^:private emitter (emitter/new))

(def ^:private accept->stringify
    (comp #(or % json/stringify)
          {"application/edn"     pr-str
           "application/transit" transit/stringify}))

(defn sub [{:keys [query-params headers] :as request}]
    (when (= "websocket" (get headers "upgrade"))
        (let [stringify (accept->stringify (get query-params "accept"))
              chan (async/chan 100)]
            (httpkit/with-channel request websocket
                (emitter/on emitter chan)
                (async/go-loop [[event data] (async/<! chan)]
                    (httpkit/send! websocket (stringify {:event event :data data}))
                    (recur (async/<! chan)))
                (httpkit/on-close websocket (fn [_]
                                                (async/close! chan)))))))

(def publish (partial emitter/publish emitter))
