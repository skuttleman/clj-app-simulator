(ns com.ben-allred.app-simulator.ui.simulators.file.interactions
  (:require
    [com.ben-allred.app-simulator.templates.transformations.file :as tr]
    [com.ben-allred.app-simulator.ui.services.store.actions :as actions]
    [com.ben-allred.app-simulator.ui.services.store.core :as store]
    [com.ben-allred.app-simulator.ui.simulators.shared.interactions :as shared.interactions]
    [com.ben-allred.app-simulator.utils.chans :as ch :include-macros true]))

(defn update-simulator [form id]
  (shared.interactions/update-simulator form tr/model->source id))

(defn reset-simulator [form id]
  (shared.interactions/reset-config form tr/sim->model id :file))

(defn create-simulator [form]
  (shared.interactions/create-simulator form tr/model->source))

(defn ^:private delete-file [action]
  (fn [hide]
    (fn [_]
      (-> action
          (store/dispatch)
          (ch/->then body
            (shared.interactions/toast body :success "The resource has been deleted"))
          (ch/->catch body
            (shared.interactions/toast body :error "The resource could not be deleted"))
          (ch/finally hide)))))

(defn show-delete-modal [title msg action]
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
    (let [current-model @form]
      (-> (actions/upload-replace id files)
          (store/dispatch)
          (ch/->then body
            (shared.interactions/toast body :success "The resource has been replaced")
            (reset! form current-model))
          (ch/->catch body
            (shared.interactions/toast body :error "The resource could not be replaced"))))))

(defn upload-resources [form]
  (fn [files]
    (let [current-model @form]
      (-> files
          (actions/upload)
          (store/dispatch)
          (ch/->then body
            (shared.interactions/toast body
                                       :success
                                       "The resources have been uploaded and are available for use in a file simulator")
            (reset! form current-model))
          (ch/->catch body
            (shared.interactions/toast body
                                       :error
                                       "The resources could not be uploaded"))))))
