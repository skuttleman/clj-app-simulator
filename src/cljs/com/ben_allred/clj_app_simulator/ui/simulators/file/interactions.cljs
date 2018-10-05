(ns com.ben-allred.clj-app-simulator.ui.simulators.file.interactions
  (:require [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as shared.interactions]
            [com.ben-allred.clj-app-simulator.templates.transformations.file :as tr]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.modals :as modals]))

(defn update-simulator [form id submittable?]
  (shared.interactions/update-simulator form tr/model->source id submittable?))

(defn reset-simulator [form id]
  (shared.interactions/reset-simulator form tr/sim->model id))

(defn create-simulator [form submittable?]
  (shared.interactions/create-simulator form tr/model->source submittable?))

(defn ^:private delete-file [action]
  (fn [hide]
    (fn [_]
      (shared.interactions/do-request
        (store/dispatch action)
        (comp hide (shared.interactions/toaster :success "The resource has been deleted"))
        (shared.interactions/toaster :error "The resource could not be deleted")))))

(defn show-delete-modal [title msg action]
  (fn [_]
    (store/dispatch
      (actions/show-modal
        [modals/confirm-delete msg]
        title
        [:button.button.is-danger.delete-button
         {:on-click (delete-file action)}
         "Delete"]
        [:button.button.cancel-button
         "Cancel"]))))

(defn replace-resource [id files]
  (shared.interactions/do-request
    (store/dispatch (actions/upload-replace id files))
    (shared.interactions/toaster :success "The resource has been replaced")
    (shared.interactions/toaster :error "The resource could not be replaced")))

(defn upload-resources [files]
  (shared.interactions/do-request
    (store/dispatch (actions/upload files))
    (shared.interactions/toaster :success "The resources have been uploaded and are available for use in a file simulator")
    (shared.interactions/toaster :error "The resources could not be uploaded")))
