(ns com.ben-allred.clj-app-simulator.ui.services.store.reducers
    (:require [com.ben-allred.collaj.reducers :as collaj.reducers]
              [com.ben-allred.clj-app-simulator.utils.maps :as maps :include-macros true]))

(defn page
    ([] nil)
    ([state [type page]]
     (case type
         :router/navigate page
         state)))

(defn modal
    ([] {:state :unmounted})
    ([state [type content title]]
     (case type
         :modal/mount {:state :mounted :content content :title title}
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

(def root
    (collaj.reducers/combine (maps/->map page modal toasts)))
