(ns com.ben-allred.app-simulator.ui.views.components.core
  (:require
    [com.ben-allred.app-simulator.services.forms.core :as forms]
    [com.ben-allred.app-simulator.templates.core :as templates]
    [com.ben-allred.app-simulator.templates.views.core :as views]
    [com.ben-allred.app-simulator.templates.views.forms.shared :as shared.views]
    [com.ben-allred.app-simulator.ui.utils.dom :as dom]
    [com.ben-allred.app-simulator.utils.chans :as ch]
    [com.ben-allred.app-simulator.utils.colls :as colls]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [reagent.core :as r]))

(defn spinner-overlay [show? component]
  (if show?
    [:div
     {:style {:position :relative}}
     [:div.spinner-container
      {:style {:position :absolute :height "50%" :min-height "200px" :min-width "100%"}}
      [views/spinner]]
     [:div.component-container
      {:style {:position :absolute :height "100%" :min-height "400px" :min-width "100%" :background-color "rgba(0,0,0,0.25)"}}
      ""]
     component]
    component))

(defn with-status [component & status-data]
  (if (every? #{:available} (map :status status-data))
    (into (colls/force-sequential component) (map :data status-data))
    [views/spinner]))

(defn menu* [{:keys [open? on-click items class-name]} [btn attrs? & content]]
  (let [btn-attrs (cond-> {:aria-haspopup true
                           :aria-controls "dropdown-menu"
                           :type          :button
                           :on-click      on-click
                           :on-mouse-up   dom/stop-propagation
                           :style {:box-shadow :none}}
                    (map? attrs?) (merge attrs?))
        icon (if open? :i.fa.fa-angle-up :i.fa.fa-angle-down)
        content (cond->> content
                  (not (map? attrs?)) (cons attrs?))]
    [:div.dropdown
     (-> {:class-name class-name}
         (templates/classes {:is-active open?}))
     [:div.dropdown-trigger
      (-> [btn btn-attrs]
          (into content)
          (conj [icon {:aria-hidden true :style {:padding-left "5px"}}]))]
     [:div#dropdown-menu.dropdown-menu
      {:role :menu}
      [:div.dropdown-content
       (for [{:keys [href label]} items]
         [:a.dropdown-item {:href href :key label} label])]]]))

(defn menu [_attrs _button]
  (let [open? (r/atom false)
        listener (atom nil)]
    (reset! listener (dom/add-listener js/window "mouseup" #(reset! open? false)))
    (r/create-class
      {:component-will-unmount
       (fn [_]
         (dom/remove-listener @listener))
       :reagent-render
       (fn [attrs button]
         [menu*
          (assoc attrs :on-click #(swap! open? not) :open? @open?)
          button])})))

(defn upload [_attrs]
  (let [key (name (gensym))]
    (fn [{:keys [class-name on-change single? form static-content persisting-content]}]
      (if (forms/syncing? form)
        [:button.is-disabled.button.file-cta
         {:class-name class-name
          :key        key
          :disabled   true}
         [:span
          {:style {:display :flex :align-items :center}}
          [:span.file-icon
           [:i.fa.fa-upload]]
          [:span.is-disabled
           {:style    {:display :flex :align-items :center}
            :disabled true}
           persisting-content
           [:span
            {:style {:margin-left "10px"}}
            [views/spinner]]]]]
        [:div.file
         {:class-name class-name
          :key        key}
         [:label.label
          [:input.file-input
           (-> {:type      :file
                :on-change #(let [target (.-target %)
                                  files (.-files target)]
                              (-> (for [i (range (.-length files))]
                                    (aget files i))
                                  (on-change)
                                  (ch/peek* (fn [_]
                                              (set! (.-files target) nil)
                                              (set! (.-value target) nil)))))
                :multiple  (not single?)}
               (shared.views/with-sync-action form :on-change))]
          [:span.button.file-cta
           {:class-name class-name}
           [:span.file-icon
            [:i.fa.fa-upload]]
           [:span.file-label
            static-content]]]]))))
