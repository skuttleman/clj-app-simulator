(ns com.ben-allred.app-simulator.ui.services.forms.standard
  (:require
    [com.ben-allred.app-simulator.services.forms.core :as forms]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [reagent.core :as r]))

(defn ^:private diff-paths [paths path old-model new-model]
  (reduce-kv (fn [paths k v]
               (let [path (conj path k)]
                 (cond
                   (map? v) (diff-paths paths path (get old-model k) v)
                   (not= (get old-model k) v) (conj paths path)
                   :else paths)))
             paths
             new-model))

(defn ^:private swap* [{:keys [current] :as state} validator f f-args]
  (let [model (apply f current f-args)]
    (-> state
        (assoc :current model :errors (validator model))
        (update :touched diff-paths [] current model))))

(defn ^:private init [model validator]
  {:current   model
   :errors    (validator model)
   :initial   model
   :syncing?  false
   :touched   #{}
   :touched?  false
   :tried?    false
   :verified? false})

(defn create
  ([model]
   (create model (constantly nil)))
  ([model validator]
   (let [state (r/atom (init model validator))]
     (reify
       forms/ISync
       (ready! [_ result]
         (swap! state assoc :syncing? false))
       (sync! [_]
         (swap! state assoc :syncing? true))
       (syncing? [_]
         (:syncing? @state))

       forms/IChange
       (changed? [_]
         (let [{:keys [initial current]} @state]
           (not= initial current)))
       (changed? [_ path]
         (let [{:keys [initial current]} @state]
           (not= (get-in initial path)
                 (get-in current path))))
       (touched? [_]
         (let [{:keys [touched touched?]} @state]
           (boolean (or touched? (seq touched)))))
       (touched? [_ path]
         (let [{:keys [touched touched?]} @state]
           (or touched?
               (contains? touched path))))

       forms/IValidate
       (errors [_]
         (:errors @state))
       (valid? [_]
         (empty? (:errors @state)))

       forms/ITry
       (try! [_]
         (swap! state assoc :tried? true))
       (tried? [_]
         (:tried? @state))

       ISwap
       (-swap! [_ f]
         (swap! state swap* validator f nil))
       (-swap! [_ f a]
         (swap! state swap* validator f [a]))
       (-swap! [_ f a b]
         (swap! state swap* validator f [a b]))
       (-swap! [_ f a b xs]
         (swap! state swap* validator f (cons a (cons b xs))))

       IReset
       (-reset! [_ model]
         (reset! state (init model validator)))

       IDeref
       (-deref [_]
         (:current @state))))))
