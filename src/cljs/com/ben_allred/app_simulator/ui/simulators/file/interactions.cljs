(ns com.ben-allred.app-simulator.ui.simulators.file.interactions
  (:require
    [com.ben-allred.app-simulator.templates.transformations.file :as tr]
    [com.ben-allred.app-simulator.ui.services.store.actions :as actions]
    [com.ben-allred.app-simulator.ui.services.store.core :as store]
    [com.ben-allred.app-simulator.ui.simulators.shared.interactions :as shared.interactions]
    [com.ben-allred.app-simulator.utils.chans :as ch :include-macros true]))

(defn update-simulator [form id]
  (shared.interactions/update-simulator form tr/model->source tr/source->model id))

(defn reset-simulator [id]
  (shared.interactions/reset-config tr/source->model id :file))

(defn create-simulator [form]
  (shared.interactions/create-simulator form tr/model->source))

(defn ^:private delete-file [action]
  (fn [hide]
    (fn [_]
      (-> action
          (store/dispatch)
          (ch/peek #(shared.interactions/toast % :success "The resource has been deleted")
                   #(shared.interactions/toast % :error "The resource could not be deleted"))
          (ch/finally hide)))))

(defn show-delete-modal [title msg action]
  (fn [_]
    (store/dispatch
      (actions/show-modal
        [:modals/confirm-delete msg]
        title
        [:button.button.is-danger.delete-button
         {:id "modals/primary-action"
          :on-click (delete-file action)}
         "Delete"]
        [:button.button.cancel-button
         "Cancel"]))))

(defn replace-resource [id]
  (fn [files]
    (-> (actions/upload-replace id files)
        (store/dispatch)
        (ch/peek #(shared.interactions/toast % :success "The resource has been replaced")
                 #(shared.interactions/toast % :error "The resource could not be replaced")))))

(defn upload-resources [files]
  (-> files
      (actions/upload)
      (store/dispatch)
      (ch/peek #(shared.interactions/toast %
                                           :success
                                           "The resources have been uploaded and are available for use in a file simulator")
               #(shared.interactions/toast %
                                           :error
                                           "The resources could not be uploaded"))))
