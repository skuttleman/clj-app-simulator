(ns com.ben-allred.clj-app-simulator.ui.views.simulator
  (:require [clojure.string :as string]
            [com.ben-allred.clj-app-simulator.services.http :as http]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]
            [com.ben-allred.clj-app-simulator.utils.fns :as fns :include-macros true]
            [com.ben-allred.clj-app-simulator.ui.services.forms.fields :as fields]
            [com.ben-allred.clj-app-simulator.ui.utils.moment :as mo]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.utils.strings :as strings]
            [com.ben-allred.formation.core :as f]
            [com.ben-allred.clj-app-simulator.ui.utils.dom :as dom]
            [cljs.core.async :as async]))

(defn ^:private maybe-parse-int [value]
  (let [delay (js/parseInt value)
        delay-str (str delay)
        trim-zeros (string/replace value #"^0+" "")]
    (if (or (= trim-zeros delay-str)
            (= (str 0 trim-zeros) delay-str))
      delay
      value)))

(def ^:private statuses
  (->> http/kw->status
       (map (juxt second #(str (strings/titlize (name (first %)) " ") " [" (second %) "]")))
       (sort-by first)))

(def ^:private source->model
  (f/make-transformer
    [#(update % :delay (fn [delay] (or delay 0)))
     {:response {:headers (fns/=>> (mapcat (fn [[k v]]
                                             (if (coll? v)
                                               (->> v
                                                    (sort)
                                                    (map (fn [v'] [k v'])))
                                               [[k v]])))
                                   (sort-by first)
                                   (vec))}}]))

(def ^:private model->source
  (f/make-transformer
    {:response    {:headers #(reduce (fn [m [k v]]
                                       (let [existing (get m k)
                                             trimmed (strings/trim-to-nil v)]
                                         (cond
                                           (not trimmed) m
                                           (string? existing) (assoc m k [(get m k) trimmed])
                                           (coll? existing) (update m k conj trimmed)
                                           :else (assoc m k trimmed))))
                                     {}
                                     %)
                   :body    strings/trim-to-nil}
     :name        strings/trim-to-nil
     :group       strings/trim-to-nil
     :description strings/trim-to-nil}))

(def ^:private model->view
  {:delay    str
   :response {:status  str
              :headers (f/transformer-tuple
                         [name strings/titlize]
                         identity)}})
(def ^:private view->model
  {:delay    maybe-parse-int
   :response {:status  js/parseInt
              :headers (f/transformer-tuple
                         [string/lower-case keyword]
                         identity)}})

(defn ^:private config->model [config]
  (-> config
      (select-keys #{:group :response :delay :name :description})
      (source->model)))

(defn ^:private with-attrs [attrs form path]
  (assoc attrs
    :on-change (partial forms/assoc-in form path)
    :value (get-in (forms/current-model form) path)
    :to-view (get-in model->view path)
    :to-model (get-in view->model path)
    :errors (get-in (forms/errors form) path)))

(defn validate* []
  (f/make-validator
    {:delay    [(f/pred #(and (not (js/isNaN %)) (number? %)) "Delay must be a number")
                (f/pred #(= (js/parseInt %) %) "Delay must be a whole number")
                (f/pred #(>= % 0) "Delay cannot be negative")]
     :response {:status  (f/required "Must have a status")
                :headers (f/validator-coll
                           (f/validator-tuple
                             (f/pred (comp (partial re-matches #"[A-Za-z0-9_-]+") name) "Invalid header key")
                             (f/pred #(seq %) "Header must have a value")))}}))

(def ^:private validate (validate*))

(defn reset-sim [request! reset!]
  (async/go
    (let [[status body] (async/<! (request!))]
      (when (= :success status)
        (reset! (config->model (:config body)))))))

(defn sim-iterate
  ([label m class]
   (sim-iterate label m class identity))
  ([label m class xform-key]
   [:div
    {:class-name class}
    [:h5 label]
    [:ul.key-vals
     (for [[k v] (sort-by key m)]
       [:li
        {:key (str k)}
        [:span.key (xform-key (name k))]
        ": "
        [:span.val (if (coll? v)
                     (string/join "," v)
                     v)]])]]))

(defn sim-details [{{:keys [method path]} :config}]
  [:div.sim-card-identifier
   [:div.sim-card-method (string/upper-case (name method))]
   [:div.sim-card-path path]])

(defn name-field [form]
  [fields/input
   (-> {:label "Name"}
       (with-attrs form [:name]))])

(defn group-field [form]
  [fields/input
   (-> {:label "Group"}
       (with-attrs form [:group]))])

(defn description-field [form]
  [fields/textarea
   (-> {:label "Description"}
       (with-attrs form [:description]))])

(defn status-field [form]
  [fields/select
   (-> {:label "Status"}
       (with-attrs form [:response :status]))
   statuses])

(defn delay-field [form]
  [fields/input
   (-> {:label "Delay (ms)"}
       (with-attrs form [:delay]))])

(defn headers-field [form]
  [fields/multi
   (-> {:label     "Headers"
        :key-fn    #(str "header-" (first %))
        :new-fn    (constantly ["" ""])
        :change-fn #(apply forms/update-in form [:response :headers] %&)}
       (with-attrs form [:response :headers])
       (dissoc :on-change))
   fields/header])

(defn body-field [form]
  [fields/textarea
   (-> {:label "Body"}
       (with-attrs form [:response :body]))])

(defn sim-edit-form* [id form]
  (let [disabled? (or (forms/errors form) (not (forms/changed? form)))]
    [:form.simulator-edit
     {:on-submit #(let [current-model (forms/current-model form)]
                    (dom/prevent-default %)
                    (when-not disabled?
                      (->> current-model
                           (model->source)
                           (actions/update-simulator id)
                           (store/dispatch))
                      (forms/reset! form current-model)))}
     [name-field form]
     [group-field form]
     [description-field form]
     [status-field form]
     [delay-field form]
     [headers-field form]
     [body-field form]
     [:div.button-row
      [:button.button.button-warning.pure-button.reset-button
       {:type     :button
        :on-click #(reset-sim (comp store/dispatch (partial actions/reset-simulator id))
                              (partial forms/reset! form))}
       "Reset"]
      [:button.button.button-secondary.pure-button.save-button
       {:disabled disabled?}
       "Save"]]]))

(defn sim-edit-form [{:keys [config id]}]
  (let [form (-> config
                 (config->model)
                 (forms/create validate))]
    (fn [_simulator]
      [sim-edit-form* id form])))

(defn request-modal [{:keys [method path]} {:keys [dt query-params headers body]}]
  [:div.request-details
   [:p (string/upper-case (name method)) ": " path]
   [:p (mo/format dt)]
   (when (seq query-params)
     [sim-iterate "Query:" query-params "query-params"])
   (when (seq headers)
     [sim-iterate "Headers:" headers "headers" strings/titlize])
   (when (seq body)
     [:div.request-body
      [:span "Body:"]
      [:p body]])])

(defn sim-request [sim {:keys [timestamp] :as request}]
  (let [dt (mo/->moment timestamp)]
    [:li.request
     {:on-click #(store/dispatch
                   (actions/show-modal
                     [request-modal sim (assoc request :dt dt)]
                     "Request Details"))}
     [:div
      (mo/from-now dt)]]))

(defn sim [{:keys [config requests id] :as simulator}]
  [:div.simulator
   [:h3 "Simulator"]
   [sim-details simulator]
   [sim-edit-form simulator]
   [:h4 "Requests:"]
   [:ul.requests
    (for [request (sort-by :timestamp > requests)]
      ^{:key (str (:timestamp request))}
      [sim-request config request])]
   [:button.button.button-error.pure-button.clear-button
    {:disabled (empty? requests)
     :on-click #(store/dispatch (actions/clear-requests id))}
    "Clear Requests"]])
