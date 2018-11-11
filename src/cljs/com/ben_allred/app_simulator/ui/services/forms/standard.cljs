(ns com.ben-allred.app-simulator.ui.services.forms.standard
  (:require
    [com.ben-allred.app-simulator.ui.services.forms.core :as forms]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [reagent.core :as r]))

(defn ^:private change [state path f]
  (-> state
      (update-in (cons :current path) f)
      (update :dirty-fields conj path)))

(defn ^:private init [model]
  {:current      model
   :dirty-fields #{}
   :initial      model
   :syncing?     false
   :verified?    false})

(defn create
  ([model]
   (create model nil))
  ([model validator]
   (let [state (r/atom (init model))]
     (reify
       forms/ISync
       (ready! [_]
         (swap! state assoc :syncing? false))
       (sync! [_]
         (swap! state assoc :syncing? true))
       (syncing? [_]
         (:syncing? @state))

       forms/IChange
       (touched? [_]
         (boolean (seq (:dirty-fields @state))))
       (touched? [_ path]
         (contains? (:dirty-fields @state) path))
       (changed? [_]
         (let [{:keys [initial current]} @state]
           (not= initial current)))
       (changed? [_ path]
         (let [{:keys [initial current]} @state]
           (not= (get-in initial path)
                 (get-in current path))))
       (assoc-in! [_ path value]
         (swap! state change path (constantly value)))
       (-update-in! [_ path f f-args]
         (swap! state change path #(apply f % f-args)))

       forms/IVerify
       (verify! [_]
         (swap! state assoc :verified? true))
       (verified? [_]
         (:verified? @state))

       forms/IValidate
       (errors [_]
         (when validator
           (validator (:current @state))))

       IReset
       (-reset! [_ model]
         (reset! state (init model)))

       IDeref
       (-deref [_]
         (:current @state))))))
