(ns com.ben-allred.clj-app-simulator.ui.services.store.activity
  (:require
    [com.ben-allred.clj-app-simulator.services.env :as env]
    [com.ben-allred.clj-app-simulator.services.ws :as ws]
    [com.ben-allred.clj-app-simulator.utils.logging :as log]
    [com.ben-allred.clj-app-simulator.utils.transit :as transit]))

(def ^:private event->action
  {:resources/put            :files.replace/succeed
   :resources/clear          :files.delete-all/succeed
   :resources/remove         :files.delete/succeed
   :simulators/change        :simulators.activity/change
   :simulators/delete        :simulators.activity/delete
   :simulators/add           :simulators.activity/add
   :simulators/init          :simulators.fetch-all/succeed
   :simulators/receive       :simulators.activity/receive
   :simulators/reset-all     :simulators.fetch-all/succeed
   :simulators/reset         :simulators.activity/reset
   :simulators.ws/connect    :simulators.activity/connect
   :simulators.ws/disconnect :simulators.activity/disconnect})

(defn ^:private on-msg [dispatch {:keys [event data]}]
  (if-let [action (event->action event)]
    (dispatch [action data])
    (when (env/get :dev?)
      (js/console.log [:activity/unknown [event data]]))))

(defn ^:private reconnect [dispatch]
  (let [host (env/get :host)
        protocol (if (= (env/get :protocol) :https)
                   :wss
                   :ws)]
    (ws/connect (str (name protocol) "://" host "/api/simulators/activity")
                :query-params {:accept "application/transit"}
                :to-string transit/stringify
                :to-clj transit/parse
                :on-msg (partial on-msg dispatch)
                :on-err (partial reconnect dispatch))))

(defn sub [{:keys [dispatch] :as store}]
  (reconnect dispatch)
  store)
