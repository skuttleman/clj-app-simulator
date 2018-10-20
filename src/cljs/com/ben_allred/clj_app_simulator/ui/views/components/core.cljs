(ns com.ben-allred.clj-app-simulator.ui.views.components.core
  (:require
    [com.ben-allred.clj-app-simulator.templates.core :as templates]
    [com.ben-allred.clj-app-simulator.templates.views.core :as views]
    [com.ben-allred.clj-app-simulator.ui.utils.dom :as dom]
    [com.ben-allred.clj-app-simulator.utils.colls :as colls]
    [com.ben-allred.clj-app-simulator.utils.logging :as log]
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
  (let [btn-attrs (cond-> {:aria-haspopup true :aria-controls "dropdown-menu"
                           :on-click      on-click}
                    (map? attrs?) (merge attrs?))
        content (cond->> content
                  (not (map? attrs?)) (cons attrs?))]
    [:div.dropdown
     (-> {:class-name class-name}
         (templates/classes {:is-active open?}))
     [:div.dropdown-trigger
      (into [btn btn-attrs] (concat content [" " [:i.fa.fa-angle-down {:aria-hidden true}]]))]
     [:div#dropdown-menu.dropdown-menu {:role :menu}
      [:div.dropdown-content
       (for [{:keys [href label]} items]
         [:a.dropdown-item {:href href :key label} label])]]]))

(defn menu [_attrs _button]
  (let [open? (r/atom false)]
    (fn [attrs button]
      [menu*
       (assoc attrs :on-click #(swap! open? not) :open? @open?)
       button])))

(defn upload [{:keys [class-name on-change multiple] :or {multiple true}} & children]
  [:div.file
   {:class-name class-name}
   [:label.label
    [:input.file-input
     {:type      :file
      :on-change #(let [target (.-target %)
                        files (.-files target)]
                    (on-change (for [i (range (.-length files))]
                                 (aget files i)))
                    (set! (.-files target) nil)
                    (set! (.-value target) nil))
      :multiple  multiple}]
    [:span.file-cta
     [:span.file-icon
      [:i.fa.fa-upload]]
     (into [:span.file-label] children)]]])
