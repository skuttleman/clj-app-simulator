(ns com.ben-allred.clj-app-simulator.ui.views.resources
  (:require
    [com.ben-allred.clj-app-simulator.templates.views.resources :as views.res]
    [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
    [com.ben-allred.clj-app-simulator.ui.simulators.file.interactions :as interactions]
    [com.ben-allred.clj-app-simulator.ui.views.components.core :as components]
    [com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]))

(defn resource [_resource]
  (let [form (forms/create {})]
    (fn [{:keys [id] :as resource}]
      [views.res/resource
       {:disabled (forms/syncing? form)
        :on-click (interactions/show-delete-modal "Delete Resource" "this resource" (actions/delete-upload id))}
       resource
       [components/upload
        {:on-change          (interactions/replace-resource form id)
         :class-name         "is-warning"
         :single?            true
         :sync-fn            #(forms/syncing? form)
         :static-content     "Replace"
         :persisting-content "Replacing"}]])))

(defn root [_resources]
  (let [form (forms/create {})]
    (fn [resources]
      [views.res/resources
       {:disabled (or (empty? resources) (forms/syncing? form))
        :on-click (interactions/show-delete-modal "Delete All Resources" "all resources" actions/delete-uploads)}
       [components/upload
        {:on-change          (interactions/upload-resources form)
         :class-name         "is-primary"
         :sync-fn            #(forms/syncing? form)
         :static-content     "Upload"
         :persisting-content "Uploading"}]
       resource
       resources])))
