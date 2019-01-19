(ns com.ben-allred.app-simulator.ui.services.forms.standard
  (:require
    [com.ben-allred.app-simulator.services.forms.core :as forms]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [com.ben-allred.app-simulator.utils.maps :as maps]
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

(defn ^:private nest [paths path model]
  (reduce-kv (fn [paths k v]
               (let [path (conj path k)]
                 (if (map? v)
                   (nest paths path v)
                   (assoc paths path v))))
             paths
             model))

(defn ^:private roll-up [errors sync-state]
  (reduce-kv assoc-in errors sync-state))

(defn ^:private model->trackable [model]
  (->> model
       (nest {} [])
       (maps/map-vals (fn [value]
                        {:current  value
                         :initial  value
                         :touched? false}))))

(defn ^:private trackable->model [trackable]
  (reduce-kv (fn [model path {:keys [current]}]
               (assoc-in model path current))
             {}
             trackable))

(defn ^:private check-for [working pred]
  (loop [[val :as working] (vals working)]
    (if (empty? working)
      false
      (or (pred val) (recur (rest working))))))

(defn ^:private swap* [{:keys [working] :as state} validator f f-args]
  (let [current (trackable->model working)
        next (apply f current f-args)
        working (->> next
                     (diff-paths #{} [] current)
                     (reduce (fn [working path]
                               (update working path assoc
                                       :current (get-in next path)
                                       :touched? true))
                             working))]
    (-> state
        (assoc :working working :errors (validator next)))))

(defn ^:private init [model validator]
  {:working (model->trackable model)
   :errors    (validator model)
   :syncing?  false
   :tried?    false})

(defn create
  ([model]
   (create model (constantly nil)))
  ([model validator]
   (let [state (r/atom (init model validator))]
     (reify
       forms/ISync
       (ready! [_]
         (swap! state assoc :syncing? false))
       (ready! [this status result]
         (if (= :success status)
           (reset! state (init result validator))
           (swap! state assoc
                  :syncing? false
                  :server-errors (nest {} [] (:errors result))
                  :sync-state @this)))
       (sync! [_]
         (swap! state assoc :syncing? true))
       (syncing? [_]
         (:syncing? @state))

       forms/IChange
       (touch! [_ path]
         (swap! state assoc-in [:working path :touched?] true))
       (changed? [_]
         (check-for (:working @state) #(not= (:initial %) (:current %))))
       (changed? [_ path]
         (let [{:keys [current initial]} (get-in @state [:working path])]
            (= current initial)))
       (touched? [_]
         (check-for (:working @state) :touched?))
       (touched? [_ path]
         (get-in @state [:working path :touched?]))

       forms/IValidate
       (errors [this]
         (let [{:keys [errors server-errors sync-state]} @state]
           (cond-> errors
             (and server-errors (= sync-state @this))
             (roll-up server-errors))))
       (valid? [this]
         (empty? (forms/errors this)))

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
         (trackable->model (:working @state)))))))
