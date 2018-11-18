(ns com.ben-allred.app-simulator.ui.views.resources
  (:require
    [com.ben-allred.app-simulator.services.forms.core :as forms]
    [com.ben-allred.app-simulator.templates.views.resources :as views.res]
    [com.ben-allred.app-simulator.ui.services.forms.standard :as form.std]
    [com.ben-allred.app-simulator.ui.services.store.actions :as actions]
    [com.ben-allred.app-simulator.ui.simulators.file.interactions :as interactions]
    [com.ben-allred.app-simulator.ui.views.components.core :as components]))

(defn resource [_resource]
  (let [form (form.std/create {})]
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
  (let [form (form.std/create {})]
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
