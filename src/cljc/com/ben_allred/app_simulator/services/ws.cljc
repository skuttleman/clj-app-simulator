(ns com.ben-allred.app-simulator.services.ws
  (:require
    [#?(:clj gniazdo.core :cljs com.ben-allred.app-simulator.ui.services.ws-impl) :as ws*]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [com.ben-allred.app-simulator.utils.query-params :as qp]))

(defn connect [url & {:keys [on-open on-close on-msg on-err query-params to-string to-clj]
                      :or   {on-open   identity
                             on-close  identity
                             on-msg    identity
                             on-err    identity
                             to-string str
                             to-clj    identity}}]
  (let [uri (cond-> url
              (seq query-params) (str "?" (qp/stringify query-params)))
        closed-atom (atom true)]
    (with-meta
      [(ws*/connect uri
                    :on-connect (fn [event]
                                  (reset! closed-atom false)
                                  (on-open event))
                    :on-receive (comp on-msg to-clj)
                    :on-error on-err
                    :on-close (fn [event & more]
                                (reset! closed-atom true)
                                (on-close #?(:clj  (into [event] more)
                                             :cljs [(.-code event) (.-reason event)]))))]
      {::to-string to-string
       ::closed?   closed-atom})))

(defn send! [[ws :as socket] msg]
  (let [to-string (::to-string (meta socket))]
    (ws*/send-msg ws (to-string msg))))

(defn close! [[ws]]
  (ws*/close ws))

(defn closed? [socket]
  (some-> socket
          (meta)
          (::closed?)
          (deref)))
