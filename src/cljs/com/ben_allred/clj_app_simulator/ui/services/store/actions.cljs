(ns com.ben-allred.clj-app-simulator.ui.services.store.actions
  (:require [com.ben-allred.clj-app-simulator.services.http :as http]
            [cljs.core.async :as async]
            [com.ben-allred.clj-app-simulator.ui.utils.macros :as macros :include-macros true]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.services.files :as files]))

(defn ^:private ->event [type value]
  (if (vector? type)
    (conj type value)
    [type value]))

(defn request* [request dispatch success-type error-type]
  (async/go
    (let [[status result :as response] (async/<! request)]
      (dispatch (if (= :success status)
                  (->event success-type result)
                  (->event error-type result)))
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

(defn clear-requests [action id]
  (fn [[dispatch]]
    (dispatch [:simulators.clear-requests/request])
    (-> (str "/api/simulators/" id)
        (http/patch {:body {:action action}})
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
        (http/patch {:body {:action :simulators/change :config config}})
        (request* dispatch :simulators.change/succeed :simulators.change/fail))))

(defn disconnect [simulator-id socket-id]
  (fn [[dispatch]]
    (dispatch [:simulators.disconnect/request])
    (-> (str "/api/simulators/" simulator-id)
        (http/patch {:body {:action :simulators.ws/disconnect :socket-id socket-id}})
        (request* dispatch :simulators.disconnect/succeed :simulators.disconnect/fail))))

(defn disconnect-all [id]
  (fn [[dispatch]]
    (dispatch [:simulators.disconnect-all/request])
    (-> (str "/api/simulators/" id)
        (http/patch {:body {:action :simulators.ws/disconnect-all}})
        (request* dispatch :simulators.disconnect-all/succeed :simulators.disconnect-all/fail))))

(defn send-message [simulator-id socket-id message]
  (fn [[dispatch]]
    (dispatch [:simulators.send-message/request])
    (let [socket-path (when socket-id (str "/sockets/" socket-id))]
      (-> (str "/api/simulators/" simulator-id socket-path)
          (http/post {:body message :headers {:content-type "text/plain"}})
          (request* dispatch :simulators.send-message/succeed :simulators.send-message/fail)))))

(defn upload [files]
  (fn [[dispatch]]
    (dispatch [:files.upload/request])
    (-> "/api/resources"
        (files/upload :post files)
        (request* dispatch :files.upload/succeed :files.upload/fail))))

(defn upload-replace [id files]
  (fn [[dispatch]]
    (dispatch [:files.replace/request])
    (-> (str "/api/resources/" id)
        (files/upload :put files)
        (request* dispatch :files.replace/succeed :files.replace/fail))))

(defn get-uploads [[dispatch]]
  (dispatch [:files.fetch-all/request])
  (-> "/api/resources"
      (http/get)
      (request* dispatch :files.fetch-all/succeed :files.fetch-all/fail)))

(defn delete-upload [id]
  (fn [[dispatch]]
    (dispatch [:files.delete/request])
    (-> (str "/api/resources/" id)
        (http/delete)
        (request* dispatch [:files.delete/succeed {:id id}] :files.delete/fail))))

(defn delete-uploads [[dispatch]]
  (dispatch [:files.delete-all/request])
  (-> "/api/resources"
      (http/delete)
      (request* dispatch :files.delete-all/succeed :files.delete-all/fail)))

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
