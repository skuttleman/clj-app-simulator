(ns com.ben-allred.clj-app-simulator.ui.services.store.reducers
  (:require [com.ben-allred.collaj.reducers :as collaj.reducers]
            [com.ben-allred.clj-app-simulator.utils.maps :as maps :include-macros true]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(defn ^:private with-status [m reducer]
  (fn
    ([] {:status :init :data (reducer)})
    ([state [type :as action]]
     (let [status (->> m
                       (filter (comp #(% type) val))
                       (map key)
                       (first))]
       (cond-> state
         :always (update :data reducer action)
         status (assoc :status status))))))

(defn page
  ([] nil)
  ([state [type page]]
   (case type
     :router/navigate page
     state)))

(defn modal
  ([] {:state :unmounted})
  ([state [type content title actions]]
   (case type
     :modal/mount {:state :mounted :content content :title title :actions actions}
     :modal/show (assoc state :state :shown)
     :modal/hide (assoc state :state :modal-hidden)
     :modal/unmount {:state :unmounted}
     state)))

(defn toasts
  ([] {})
  ([state [type key level text]]
   (case type
     :toast/display (assoc state key {:level level :text text})
     :toast/remove (dissoc state key)
     state)))

(def simulators
  (with-status {:pending   #{:simulators.fetch-one/request :simulators.fetch-all/request}
                :available #{:simulators.fetch-one/succeed :simulators/clear :simulators.activity/receive
                             :simulators.activity/add :simulators.activity/delete :simulators.activity/reset-requests}
                :failed    #{:simulators.fetch-all/fail :simulators.fetch-one/fail}}
               (let [reducer (collaj.reducers/map-of
                               (comp :id :simulator second)
                               (fn
                                 ([] nil)
                                 ([state [type {:keys [simulator request]}]]
                                  (case type
                                    :simulators.fetch-one/succeed simulator
                                    :simulators.activity/receive (update state :requests (fnil conj []) request)
                                    :simulators.activity/reset-requests (assoc state :requests [])
                                    :simulators.activity/add simulator
                                    state))))]
                 (fn
                   ([] (reducer))
                   ([state [type {id :id} :as action]]
                    (case type
                      :simulators/clear (reducer)
                      :simulators.activity/delete (dissoc state id)
                      (reducer state action)))))))

(def root
  (collaj.reducers/combine (maps/->map page modal toasts simulators)))
