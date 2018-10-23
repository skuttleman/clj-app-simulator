(ns com.ben-allred.clj-app-simulator.ui.views.resources
  (:require
    [com.ben-allred.clj-app-simulator.templates.views.resources :as views.res]
    [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
    [com.ben-allred.clj-app-simulator.ui.simulators.file.interactions :as interactions]
    [com.ben-allred.clj-app-simulator.ui.views.components.core :as components]))

(defn resource [{:keys [id] :as resource}]
  [views.res/resource
   {:on-click (interactions/show-delete-modal "Delete Resource" "this resource" (actions/delete-upload id))}
   resource
   [components/upload
    {:on-change  (partial interactions/replace-resource id)
     :class-name "is-warning"
     :multiple   false}]])

(defn root [resources]
  [views.res/resources
   {:disabled (empty? resources)
    :on-click (interactions/show-delete-modal "Delete All Resources" "all resources" actions/delete-uploads)}
   [components/upload
    {:on-change  interactions/upload-resources
     :class-name "is-primary"}]
   resource
   resources])
