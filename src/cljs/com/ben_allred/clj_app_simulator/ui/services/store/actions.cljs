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

(def request-user-details
    (fn [[dispatch]]
        (dispatch [:user/request])
        (-> "/auth/details"
            (http/get)
            (request* dispatch :user/succeed :user/fail))))

(def request-configs
    (fn [[dispatch]]
        (dispatch [:configs/request])
        (-> "/api/configs"
            (http/get)
            (request* dispatch :configs/succeed :configs/fail))))

(defn request-config [config-id]
    (fn [[dispatch]]
        (dispatch [:config/request])
        (-> "/api/configs/"
            (str config-id)
            (http/get)
            (request* dispatch :config/succeed :config/fail))))

(defn update-rules [config-id rules]
    (fn [[dispatch]]
        (dispatch [:config.rules/update])
        (-> "/api/configs/"
            (str config-id)
            (http/patch {:body {:data {:rules rules}}})
            (request* dispatch :config.rules/succeed :config.rules/fail))))

(defn update-messages [config-id messages]
    (fn [[dispatch]]
        (dispatch [:config.messages/update])
        (-> "/api/configs/"
            (str config-id)
            (http/patch {:body {:data {:messages messages}}})
            (request* dispatch :config.messages/succeed :config.messages/fail))))

(defn update-description [config-id description]
    (fn [[dispatch]]
        (dispatch [:configs.config/update])
        (-> "/api/configs/"
            (str config-id)
            (http/patch {:body {:data {:description description}}})
            (request* dispatch :configs.config/succeed :configs.config/fail))))

(defn save-repo [repo]
    (fn [[dispatch]]
        (dispatch [:configs.config.new/save])
        (-> "/api/configs"
            (http/post {:body {:data repo}})
            (request* dispatch :configs.config.new/succeed :configs.config.new/fail))))

(defn show-modal [content & [title]]
    (fn [[dispatch]]
        (dispatch [:modal/mount content title])
        (macros/after 1 (dispatch [:modal/show]))))

(def hide-modal
    (fn [[dispatch]]
        (dispatch [:modal/hide])
        (macros/after 600 (dispatch [:modal/unmount]))))

(defn show-toast [level text]
    (fn [[dispatch]]
        (let [key (gensym)]
            (dispatch [:toast/display key level text])
            (macros/after 6000 (dispatch [:toast/remove key])))))
