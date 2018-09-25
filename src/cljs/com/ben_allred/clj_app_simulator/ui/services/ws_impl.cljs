(ns com.ben-allred.clj-app-simulator.ui.services.ws-impl)

(defn connect [uri & {:keys [on-connect on-receive on-error on-close]}]
  [(doto (js/WebSocket. uri)
     (aset "onopen" on-connect)
     (aset "onmessage" (comp on-receive #(.-data %)))
     (aset "onerror" on-error)
     (aset "onclose" on-close))])

(defn send-msg [ws msg]
  (.send ws msg))

(defn close [ws]
  (.close ws))
