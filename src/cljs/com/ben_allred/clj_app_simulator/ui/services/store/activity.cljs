(ns com.ben-allred.clj-app-simulator.ui.services.store.activity
  (:require [com.ben-allred.clj-app-simulator.utils.transit :as transit]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.services.ws :as ws]
            [com.ben-allred.clj-app-simulator.services.env :as env]))

(defn ^:private on-msg [dispatch {:keys [event data]}]
  (case event
    :simulators/init (dispatch [:simulators.fetch-all/succeed {:simulators data}])
    :simulators/receive (dispatch [:simulators.activity/receive data])
    :simulators/add (dispatch [:simulators.activity/add {:simulator data}])
    :simulators/delete (dispatch [:simulators.activity/delete data])
    :simulators/reset (dispatch [:simulators.activity/reset {:simulator data}])
    :simulators/reset-requests (dispatch [:simulators.activity/reset-requests {:simulator data}])
    :simulators/change (dispatch [:simulators.activity/change {:simulator data}])
    :ws/connect (dispatch [:simulators.activity/connect {:simulator data}])
    :ws/disconnect (dispatch [:simulators.activity/disconnect {:simulator data}])
    (log/spy [:UNKNOWN-- event data])))

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
