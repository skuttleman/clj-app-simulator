(ns com.ben-allred.app-simulator.ui.simulators.file.interactions
  (:require
    [com.ben-allred.app-simulator.templates.transformations.file :as tr]
    [com.ben-allred.app-simulator.ui.services.forms.core :as forms]
    [com.ben-allred.app-simulator.ui.services.store.actions :as actions]
    [com.ben-allred.app-simulator.ui.services.store.core :as store]
    [com.ben-allred.app-simulator.ui.simulators.shared.interactions :as shared.interactions]))

(defn update-simulator [form id]
  (shared.interactions/update-simulator form tr/model->source id))

(defn reset-simulator [form id]
  (shared.interactions/reset-config form tr/sim->model id :file))

(defn create-simulator [form]
  (shared.interactions/create-simulator form tr/model->source))

(defn ^:private delete-file [action]
  (fn [hide]
    (fn [_]
      (shared.interactions/do-request
        (store/dispatch action)
        (comp hide (shared.interactions/toaster :success "The resource has been deleted"))
        (shared.interactions/toaster :error "The resource could not be deleted")))))

(defn show-delete-modal [title msg action] ;;disably
  (fn [_]
    (store/dispatch
      (actions/show-modal
        [:modals/confirm-delete msg]
        title
        [:button.button.is-danger.delete-button
         {:on-click (delete-file action)}
         "Delete"]
        [:button.button.cancel-button
         "Cancel"]))))

(defn replace-resource [form id]
  (fn [files]
    (let [current-model (forms/current-model form)]
      (forms/sync! form (gensym))
      (shared.interactions/do-request
        (store/dispatch (actions/upload-replace id files))
        (comp (shared.interactions/resetter forms/reset! form current-model)
              (shared.interactions/toaster :success "The resource has been replaced"))
        (comp (shared.interactions/resetter forms/ready! form)
              (shared.interactions/toaster :error "The resource could not be replaced"))))))

(defn upload-resources [form]
  (fn [files]
    (let [current-model (forms/current-model form)]
      (forms/sync! form (gensym))
      (shared.interactions/do-request
        (store/dispatch (actions/upload files))
        (comp (shared.interactions/resetter forms/reset! form current-model)
              (shared.interactions/toaster :success
                                           "The resources have been uploaded and are available for use in a file simulator"))
        (comp (shared.interactions/resetter forms/ready! form)
              (shared.interactions/toaster :error
                                           "The resources could not be uploaded"))))))
