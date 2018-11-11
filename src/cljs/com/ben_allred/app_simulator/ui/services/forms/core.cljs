(ns com.ben-allred.app-simulator.ui.services.forms.core
  (:require
    [com.ben-allred.app-simulator.templates.views.core :as views]
    [com.ben-allred.app-simulator.utils.fns :as fns]
    [com.ben-allred.app-simulator.utils.logging :as log]))

(defprotocol ISync
  (ready! [this])
  (sync! [this])
  (syncing? [this]))

(defprotocol IChange
  (touched? [this] [this path])
  (changed? [this] [this path])
  (assoc-in! [this path value])
  (-update-in! [this path f f-args]))

(defprotocol IVerify
  (verify! [this])
  (verified? [this]))

(defprotocol IValidate
  (errors [this]))

(defn update-in! [this path f & f-args]
  (-update-in! this path f f-args))

(defn sync-button [{:keys [disabled form sync-text text] :as attrs}]
  (let [syncing? (syncing? form)]
    [:button.button.sync-button
     (-> attrs
         (dissoc :form :text :sync-text)
         (update :disabled fns/or syncing?)
         (update :on-click
                 (fnil comp identity)
                 (fn [event]
                   (when-not (or disabled (errors form))
                     (sync! form))
                   event)))
     (if syncing?
       [:div.syncing sync-text [views/spinner]]
       text)]))
