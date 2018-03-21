(ns com.ben-allred.clj-app-simulator.ui.services.store.actions
    (:require [com.ben-allred.clj-app-simulator.services.http :as http]
              [cljs.core.async :as async]
              [com.ben-allred.clj-app-simulator.ui.utils.macros :as macros :include-macros true]))

(defn ^:private request* [request dispatch success-type error-type]
    (async/go
        (let [[status result :as response] (async/<! request)]
            (dispatch (if (= :success status)
                          [success-type result]
                          [error-type result]))
            response)))

(def request-simulators
    (fn [[dispatch]]
        (dispatch [:simulators/request])
        (-> "/api/simulators"
            (http/get)
            (request* dispatch :simulators/succeed :simulators/fail))))

(defn show-modal [content & [title]]
    (fn [[dispatch]]
        (dispatch [:modal/mount content title])
        (macros/after 1 (dispatch [:modal/show]))))

(def hide-modal
    (fn [[dispatch]]
        (dispatch [:modal/hide])
        (macros/after 600 (dispatch [:modal/unmount]))))

(defn remove-toast [key]
    [:toast/remove key])

(defn show-toast [level text]
    (fn [[dispatch]]
        (let [key (gensym)]
            (dispatch [:toast/display key level text])
            (macros/after 6000 (dispatch (remove-toast key))))))
