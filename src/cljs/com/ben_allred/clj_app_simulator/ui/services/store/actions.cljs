(ns com.ben-allred.clj-app-simulator.ui.services.store.actions
  (:require [com.ben-allred.clj-app-simulator.services.http :as http]
            [cljs.core.async :as async]
            [com.ben-allred.clj-app-simulator.ui.utils.macros :as macros :include-macros true]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(defn ^:private request* [request dispatch success-type error-type]
  (async/go
    (let [[status result :as response] (async/<! request)]
      (dispatch (if (= :success status)
                  [success-type result]
                  [error-type result]))
      response)))

(def request-simulators
  (fn [[dispatch]]
    (dispatch [:simulators.fetch-all/request])
    (-> "/api/simulators"
        (http/get)
        (request* dispatch :simulators.fetch-all/succeed :simulators.fetch-all/fail))))

(defn delete-simulator [id]
  (fn [[dispatch]]
    (dispatch [:simulators.delete/request])
    (-> (str "/api/simulators/" id)
        (http/delete)
        (request* dispatch :simulators.delete/succeed :simulators.delete/fail))))

(defn create-simulator [simulator]
  (fn [[dispatch]]
    (dispatch [:simulators.create/request])
    (-> "/api/simulators"
        (http/post {:body {:simulator simulator}})
        (request* dispatch :simulators.create/succeed :simulators.create/fail))))

(defn clear-requests [id]
  (fn [[dispatch]]
    (dispatch [:simulators.clear-requests/request])
    (-> (str "/api/simulators/" id)
        (http/patch {:body {:action :http/reset-requests}})
        (request* dispatch :simulators.clear-requests/succeed :simulators.clear-requests/fail))))

(defn reset-simulator [id]
  (fn [[dispatch]]
    (dispatch [:simulators.reset/request])
    (-> (str "/api/simulators/" id)
        (http/patch {:body {:action :simulators/reset}})
        (request* dispatch :simulators.reset/succeed :simulators.reset/fail))))

(defn update-simulator [id config]
  (fn [[dispatch]]
    (dispatch [:simulators.change/request])
    (-> (str "/api/simulators/" id)
        (http/patch {:body {:action :http/change :config config}})
        (request* dispatch :simulators.change/succeed :simulators.change/fail))))

(defn show-modal [content & [title & actions]]
  (fn [[dispatch]]
    (dispatch [:modal/mount content title actions])
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
