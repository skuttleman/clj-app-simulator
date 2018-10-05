(ns com.ben-allred.clj-app-simulator.ui.views.resources
  (:require [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.views.components.core :as components]
            [com.ben-allred.clj-app-simulator.templates.views.resources :as views.res]
            [com.ben-allred.clj-app-simulator.ui.simulators.file.interactions :as interactions]))

(defn resource [{:keys [id] :as upload}]
  [views.res/resource
   {:on-click (interactions/show-delete-modal "Delete Resource" "this resource" (actions/delete-upload id))}
   upload
   [components/upload
    {:on-change  (partial interactions/replace-resource id)
     :class-name "is-warning"
     :multiple   false}]])

(defn root [uploads]
  [views.res/resources
   {:disabled (empty? uploads)
    :on-click (interactions/show-delete-modal "Delete All Resources" "all resources" actions/delete-uploads)}
   [components/upload
    {:on-change  interactions/upload-resources
     :class-name "is-primary"}]
   resource
   uploads])
