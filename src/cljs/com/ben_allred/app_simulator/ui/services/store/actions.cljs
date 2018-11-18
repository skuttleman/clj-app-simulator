(ns com.ben-allred.app-simulator.ui.services.store.actions
  (:require
    [com.ben-allred.app-simulator.services.files :as files]
    [com.ben-allred.app-simulator.services.http :as http]
    [com.ben-allred.app-simulator.ui.utils.macros :as macros :include-macros true]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [com.ben-allred.app-simulator.utils.chans :as ch]))

(defonce ^:private toast-id (atom 0))

(defn ^:private dispatcher [dispatch type value]
  (cond
    (vector? type) (dispatch (conj type value))
    (nil? type) nil
    :else (dispatch [type value])))

(defn request*
  ([request dispatch]
   (request* request dispatch nil))
  ([request dispatch success-type]
   (request* request dispatch success-type nil))
  ([request dispatch success-type error-type]
   (-> request
       (ch/->peek [status result]
         (dispatcher dispatch (if (= :success status) success-type error-type) result)))))

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
        (request* dispatch))))

(defn create-simulator [simulator]
  (fn [[dispatch]]
    (dispatch [:simulators.create/request])
    (-> "/api/simulators"
        (http/post {:body {:simulator simulator}})
        (request* dispatch))))

(defn clear-requests [id type]
  (fn [[dispatch]]
    (dispatch [:simulators.clear-requests/request])
    (-> (str "/api/simulators/" id)
        (http/patch {:body {:action :simulators/reset :type type}})
        (request* dispatch))))

(defn reset-simulator-config [id type]
  (fn [[dispatch]]
    (dispatch [:simulators.reset/request])
    (-> (str "/api/simulators/" id)
        (http/patch {:body {:action :simulators/reset :type (keyword type :config)}})
        (request* dispatch))))

(defn update-simulator [id config]
  (fn [[dispatch]]
    (dispatch [:simulators.change/request])
    (-> (str "/api/simulators/" id)
        (http/patch {:body {:action :simulators/change :config config}})
        (request* dispatch))))

(defn disconnect [simulator-id socket-id]
  (fn [[dispatch]]
    (dispatch [:simulators.disconnect/request])
    (-> (str "/api/simulators/" simulator-id)
        (http/patch {:body {:action :simulators.ws/disconnect :socket-id socket-id}})
        (request* dispatch))))

(defn disconnect-all [id]
  (fn [[dispatch]]
    (dispatch [:simulators.disconnect-all/request])
    (-> (str "/api/simulators/" id)
        (http/patch {:body {:action :simulators.ws/disconnect}})
        (request* dispatch))))

(defn send-message [simulator-id socket-id message]
  (fn [[dispatch]]
    (dispatch [:simulators.send-message/request])
    (let [socket-path (when socket-id (str "/sockets/" socket-id))]
      (-> (str "/api/simulators/" simulator-id socket-path)
          (http/post {:body message :headers {:content-type "text/plain"}})
          (request* dispatch)))))

(defn upload [files]
  (fn [[dispatch]]
    (dispatch [:files.upload/request])
    (-> "/api/resources"
        (files/upload :post files)
        (request* dispatch))))

(defn upload-replace [id files]
  (fn [[dispatch]]
    (dispatch [:files.replace/request])
    (-> (str "/api/resources/" id)
        (files/upload :put files)
        (request* dispatch))))

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
        (request* dispatch))))

(defn delete-uploads [[dispatch]]
  (dispatch [:files.delete-all/request])
  (-> "/api/resources"
      (http/delete)
      (request* dispatch)))

(defn show-modal [content & [title & actions]]
  (fn [[dispatch]]
    (dispatch [:modal/mount content title actions])
    (macros/after 1 (dispatch [:modal/show]))))

(def hide-modal
  (fn [[dispatch]]
    (dispatch [:modal/hide])
    (macros/after 600 (dispatch [:modal/unmount]))))

(defn remove-toast [key]
  (fn [[dispatch]]
    (dispatch [:toast/removing key])
    (macros/after 501 (dispatch [:toast/remove key]))))

(defn show-toast [level text]
  (fn [[dispatch]]
    (let [key (swap! toast-id inc)
          ref (delay
                (macros/after 1 (dispatch [:toast/display key]))
                (macros/after 7000 (dispatch (remove-toast key)))
                text)]
      (dispatch [:toast/adding key level ref]))))
