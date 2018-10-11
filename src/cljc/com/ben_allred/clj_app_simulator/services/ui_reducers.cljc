(ns com.ben-allred.clj-app-simulator.services.ui-reducers
  (:require [com.ben-allred.collaj.reducers :as collaj.reducers]
            [com.ben-allred.clj-app-simulator.utils.maps :as maps :include-macros true]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.utils.colls :as colls]))

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
     :toast/adding (assoc state key {:level level :text text :adding? true})
     :toast/display (update state key dissoc :adding?)
     :toast/remove (dissoc state key)
     state)))

(def ^:private simulators-reducer
  (collaj.reducers/map-of
    (comp :id :simulator second)
    (fn
      ([] nil)
      ([state [type {:keys [simulator request]}]]
       (case type
         :simulators.activity/receive (update state :requests (fnil conj []) request)
         :simulators.activity/reset-requests (assoc state :requests [])
         :simulators.activity/change (assoc state :config (:config simulator))
         :simulators.activity/reset (assoc state :config (:config simulator))
         :simulators.fetch-one/succeed simulator
         :simulators.activity/add simulator
         :simulators.activity/connect (update state :sockets (fnil conj #{}) (:socket-id simulator))
         :simulators.activity/disconnect (update state :sockets (fnil disj #{}) (:socket-id simulator))
         state)))))

(def simulators
  (with-status
    {:pending   #{:simulators.fetch-one/request :simulators.fetch-all/request}
     :available #{:simulators.fetch-one/succeed :simulators/clear :simulators.activity/receive
                  :simulators.activity/add :simulators.activity/delete :simulators.activity/reset-requests}
     :failed    #{:simulators.fetch-all/fail :simulators.fetch-one/fail}}
    (fn
      ([] (simulators-reducer))
      ([state [type {id :id} :as action]]
       (case type
         :simulators/clear (simulators-reducer)
         :simulators.activity/delete (dissoc state id)
         (simulators-reducer state action))))))

(def uploads
  (with-status
    {:pending   #{:files.fetch-all/request}
     :available #{:files.fetch-all/succeed :files.upload/succeed :files.replace/succeed
                  :files.delete/succeed :files.delete-all/succeed}
     :failed    #{:files.fetch-all/fail}}
    (fn
      ([] [])
      ([state [type data]]
       (case type
         :files.upload/succeed (into state data)
         :files.replace/succeed (colls/replace-by :id data state)
         :files.delete/succeed (vec (remove (comp #{(:id data)} :id) state))
         :files.delete-all/succeed []
         :files.fetch-all/succeed (:uploads data)
         state)))))

(def root
  (collaj.reducers/combine (maps/->map page modal toasts simulators uploads)))