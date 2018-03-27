(ns com.ben-allred.clj-app-simulator.services.ws
    (:require #?(:clj [gniazdo.core :as gniazdo])
                      [com.ben-allred.clj-app-simulator.utils.query-params :as qp]))

(declare connect)

(defn ^:private with-reconnect [f url {:keys [delay-ms reconnect? on-reconnect] :as opts}]
    (fn [data & [reason]]
        (let [arg (if reason [data reason] data)]
            (f arg)
            (when reconnect?
                #?(:clj  (do (Thread/sleep delay-ms)
                             (when on-reconnect
                                 (on-reconnect arg))
                             (connect url opts))
                   :cljs (.setTimeout js/window
                             (fn []
                                 (when on-reconnect
                                     (on-reconnect arg))
                                 (connect url opts))
                             delay-ms))))))

(defn connect [url & {:keys [on-open on-close on-msg on-err delay-ms query-params to-string to-clj]
                      :or   {on-open   identity
                             on-close  identity
                             on-msg    identity
                             on-err    identity
                             delay-ms  1000
                             to-string str
                             to-clj    identity}
                      :as   opts}]
    (let [uri (cond-> url
                  (seq query-params) (str "?" (qp/stringify query-params)))]
        #?(:clj (with-meta [(gniazdo/connect uri
                                             :on-connect on-open
                                             :on-receive (comp on-msg to-clj)
                                             :on-error (with-reconnect on-err url opts)
                                             :on-close (with-reconnect on-close url opts))]
                           {::to-string to-string}))))

(defn send! [socket msg]
    (let [to-string (::to-string (meta socket))
          ws        (first socket)]
        #?(:clj (gniazdo/send-msg ws (to-string msg)))))

(defn close! [socket]
    #?(:clj (gniazdo/close (first socket))))
