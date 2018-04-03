(ns com.ben-allred.clj-app-simulator.services.ws
  (:require #?(:clj [gniazdo.core :as gniazdo])
                    [com.ben-allred.clj-app-simulator.utils.query-params :as qp]
                    [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(defn connect [url & {:keys [on-open on-close on-msg on-err query-params to-string to-clj]
                      :or   {on-open   identity
                             on-close  identity
                             on-msg    identity
                             on-err    identity
                             to-string str
                             to-clj    identity}}]
  (let [uri (cond-> url
              (seq query-params) (str "?" (qp/stringify query-params)))]
    (with-meta
      #?(:clj  [(gniazdo/connect uri
                                 :on-connect on-open
                                 :on-receive (comp on-msg to-clj)
                                 :on-error on-err
                                 :on-close (comp on-close vector))]
         :cljs (let [ws (js/WebSocket. uri)]
                 (set! (.-onopen on-open))
                 (set! (.-onmessage ws) (comp on-msg to-clj #(.-data %)))
                 (set! (.-onerror ws) on-err)
                 (set! (.-onclose ws) (comp on-close (juxt #(.-code %) #(.-reason %))))
                 [ws]))
      {::to-string to-string})))

(defn send! [[ws :as socket] msg]
  (let [to-string (::to-string (meta socket))]
    #?(:clj  (gniazdo/send-msg ws (to-string msg))
       :cljs (.send ws (to-string msg)))))

(defn close! [[ws]]
  #?(:clj  (gniazdo/close ws)
     :cljs (.close ws)))
