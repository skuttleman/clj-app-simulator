(ns com.ben-allred.clj-app-simulator.ui.services.forms.core
  (:refer-clojure :exclude [assoc-in reset! update-in])
  (:require
    [com.ben-allred.clj-app-simulator.utils.logging :as log]
    [reagent.core :as r]
    [reagent.ratom :as ratom]))

(defn ^:private only-dirty [errors dirty-fields]
  (reduce (fn [err path]
            (if-some [value (get-in errors path)]
              (clojure.core/assoc-in err path value)
              err))
          nil
          dirty-fields))

(defn create
  ([model]
   (create model nil))
  ([model validator]
   (let [current (r/atom model)]
     {:initial       (r/atom model)
      :dirty-fields  (r/atom #{})
      :filter-dirty? (r/atom true)
      :current       current
      :errors        (if validator
                       (ratom/make-reaction #(validator @current))
                       (delay nil))})))

(defn initial-model [{:keys [initial]}]
  @initial)

(defn current-model [{:keys [current]}]
  @current)

(defn changed? [{:keys [current initial]}]
  (not= @initial @current))

(defn errors [{:keys [errors]}]
  @errors)

(defn display-errors [{:keys [dirty-fields filter-dirty?] :as form}]
  (cond-> (errors form)
    @filter-dirty? (only-dirty @dirty-fields)))

(defn verify! [{:keys [filter-dirty?]}]
  (clojure.core/reset! filter-dirty? false))

(defn reset! [{:keys [current dirty-fields initial filter-dirty?]} model]
  (clojure.core/reset! dirty-fields #{})
  (clojure.core/reset! filter-dirty? true)
  (clojure.core/reset! initial model)
  (clojure.core/reset! current model))

(defn update-in [{:keys [current dirty-fields]} path f & f-args]
  (swap! dirty-fields conj path)
  (apply swap! current clojure.core/update-in path f f-args))

(defn assoc-in [form path value]
  (update-in form path (constantly value)))
