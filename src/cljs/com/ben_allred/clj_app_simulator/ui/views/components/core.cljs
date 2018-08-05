(ns com.ben-allred.clj-app-simulator.ui.views.components.core
  (:require [com.ben-allred.clj-app-simulator.ui.utils.core :as utils]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [reagent.core :as r]))

(defn with-height [attrs open? item-count]
  (assoc-in attrs [:style :height] (if open?
                                     (str (+ 24 (* 18 item-count)) "px")
                                     "0")))

(defn spinner []
  [:div.loader])

(defn spinner-overlay [show? component]
  (if show?
    [:div
     {:style {:position :relative}}
     [:div.spinner-container
      {:style {:position :absolute :height "50%" :min-height "200px" :min-width "100%"}}
      [spinner]]
     [:div.component-container
      {:style {:position :absolute :height "100%" :min-height "400px" :min-width "100%" :background-color "rgba(0,0,0,0.25)"}}
      ""]
     component]
    component))

(defn with-status [component & status-data]
  (if (every? #{:available} (map :status status-data))
    (into [component] (map :data status-data))
    [spinner]))

(defn menu* [{:keys [open? on-click items class-name]} button]
  [:div.dropdown-menu-wrapper
   {:class-name class-name
    :on-click   on-click}
   (conj button [:i.fa.dropdown-chevron
                 {:class-name (if open? :fa-chevron-up :fa-chevron-down)}])
   [:div.dropdown-menu
    (-> {}
        (with-height open? (count items))
        (utils/classes {:open   open?
                        :closed (not open?)}))
    [:ul.menu
     (for [[idx {:keys [href label]}] (map-indexed vector items)]
       [:li.menu-item
        {:key idx}
        [:a {:href href}
         label]])]]])

(defn menu [attrs button]
  (let [open? (r/atom false)]
    (fn [attrs button]
      [menu*
       (assoc attrs :on-click #(swap! open? not) :open? @open?)
       button])))

(defn upload [attrs & _]
  (let [id (gensym)]
    (fn [{:keys [class-name on-change]} & args]
      [:div
       [:input.file-upload.hidden
        {:id        id
         :type      :file
         :on-change #(let [target (.-target %)
                           files (.-files target)]
                       (on-change (for [i (range (.-length files))]
                                    (aget files i)))
                       (set! (.-files target) nil)
                       (set! (.-value target) nil))
         :multiple  true}]
       (into [:button
              {:class-name class-name
               :on-click   #(-> js/document
                                (.querySelector (str "#" id))
                                (.click))}]
             args)])))
