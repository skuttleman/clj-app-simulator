(ns com.ben-allred.clj-app-simulator.ui.services.forms.core
  (:refer-clojure :exclude [reset! update-in assoc-in])
  (:require [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [reagent.core :as r]
            [reagent.ratom :as ratom]))

(defprotocol IForm
  (initial-model [form])
  (current-model [form])
  (changed? [form])
  (errors [form])
  (reset! [form model])
  (assoc-in [form path value])
  (-update-in [form path f f-args]))

(defn create
  ([model]
   (create model nil))
  ([model validator]
   (let [initial (r/atom model)
         current (r/atom model)
         errors (when validator (ratom/make-reaction #(validator @current)))]
     (reify IForm
       (initial-model [_]
         @initial)
       (current-model [_]
         @current)
       (changed? [_]
         (not= @initial @current))
       (errors [_]
         (when errors @errors))
       (reset! [_ model]
         (clojure.core/reset! initial model)
         (clojure.core/reset! current model))
       (assoc-in [this path value]
         (-update-in this path (constantly value) nil))
       (-update-in [_ path f f-args]
         (if (seq path)
           (apply swap! current clojure.core/update-in path f f-args)
           (apply clojure.core/swap! current f f-args)))))))

(defn update-in [form path f & f-args]
  (-update-in form path f f-args))
