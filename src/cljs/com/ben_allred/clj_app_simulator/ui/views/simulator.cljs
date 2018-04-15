(ns com.ben-allred.clj-app-simulator.ui.views.simulator
  (:require [cljsjs.moment]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [clojure.string :as string]
            [com.ben-allred.formation.core :as f]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.services.forms.fields :as fields]
            [reagent.core :as r]
            [com.ben-allred.clj-app-simulator.services.http :as http]))

(defn ^:private parse-int-to-nil [value]
  (let [delay (js/parseInt value)]
    (if (js/isNaN delay)
      value
      delay)))

(def ^:private source->model (f/make-transformer
                               {:response
                                {:headers [seq
                                           #(mapcat (fn [[k v]]
                                                      (if (coll? v)
                                                        (map (fn [v'] [k v']) v)
                                                        [[k v]]))
                                                    %)
                                           #(sort-by first %)
                                           vec]}}))

(def ^:private model->view (f/make-transformer
                             {:delay    str
                              :response {:status  str
                                         :headers (f/transformer-coll
                                                    (f/transformer-tuple
                                                      name
                                                      identity))}}))

(def ^:private view->model (f/make-transformer
                             {:delay    parse-int-to-nil
                              :response {:status  js/parseInt
                                         :headers (f/transformer-coll
                                                    (f/transformer-tuple
                                                      [string/lower-case keyword]
                                                      identity))}}))

(def ^:private model->source (f/make-transformer
                               {:response
                                {:headers #(reduce (fn [m [k v]]
                                                     (let [existing (get m k)]
                                                       (cond
                                                         (string? existing) (assoc m k [(get m k) v])
                                                         (coll? existing) (update m k conj v)
                                                         :else (assoc m k v))))
                                                   {}
                                                   %)}}))

(def ^:private validate (f/make-validator
                          {:delay    [(f/pred integer? "Delay must be a number")
                                      (f/pred #(>= % 0) "Delay cannot be negative")]
                           :response {:status  (f/required "Must have a status")
                                      :headers (f/validator-coll
                                                 #(when-not (and (keyword? (first %))
                                                                 (seq (second %)))
                                                    [(str "Invalid header" (pr-str %))]))}}))

(defn ^:private update-form [form path v]
  (swap! form assoc-in path (get-in (view->model (assoc-in nil path v)) path)))

(defn ^:private titlize
  ([s] (titlize s "-"))
  ([s sep]
   (->> (string/split s #"-")
        (map string/capitalize)
        (string/join sep))))

(defn sim-iterate [label m class xform-key]
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
                    v)]])]])

(defn sim-details [{{:keys [method path]} :config}]
  [:div.sim-card-identifier
   [:div.sim-card-method (string/upper-case (name method))]
   [:div.sim-card-path path]])

(defn sim-edit-form [{:keys [config id]}]
  (let [initial-model (source->model (select-keys config #{:response :delay}))
        form (r/atom initial-model)]
    (fn [_simulator]
      (let [model @form
            errors (validate model)
            view-data (model->view model)]
        [:form
         {:on-submit #(do (.preventDefault %)
                          (log/spy (model->source @form)))}
         [fields/select
          {:on-change (partial update-form form [:response :status])
           :label     "Status"
           :value     (get-in view-data [:response :status])}
          (->> http/kw->status
               (map (juxt second #(str (titlize (name (first %)) " ") " [" (second %) "]")))
               (sort-by first))]
         [fields/input
          {:on-change (partial update-form form [:delay])
           :value     (:delay view-data)
           :label     "Delay (ms)"}]
         [fields/textarea
          {:on-change (partial update-form form [:response :body])
           :label     "Body"
           :value     (get-in view-data [:response :body])}]
         [:button.button.button-warning.pure-button
          {:type :button}
          "Reset"]
         [:button.button.button-secondary.pure-button
          {:disabled (or (log/spy errors) (= initial-model model))}
          "Save"]]))))

(defn sim-request [{:keys [method path]} {:keys [timestamp query-params headers body]}]
  (let [dt (js/moment timestamp)]
    [:li.request
     {:on-click #(store/dispatch (actions/show-modal [:div.request-details
                                                      [:p (string/upper-case (name method)) ": " path]
                                                      [:p (.format dt)]
                                                      (when (seq query-params)
                                                        [sim-iterate "Query:" query-params "query-params" identity])
                                                      (when (seq headers)
                                                        [sim-iterate "Headers:" headers "headers" titlize])
                                                      (when (seq body)
                                                        [:div
                                                         [:span "Body:"]
                                                         [:p body]])]
                                                     "Request Details"))}
     [:div
      (.fromNow dt)]]))

(defn sim [{:keys [config requests] :as simulator}]
  [:div.simulator
   [:h3 "Simulator"]
   [sim-details simulator]
   [sim-edit-form simulator]
   [:h4 "Requests:"]
   [:ul.requests
    (for [request (sort-by :timestamp > requests)]
      ^{:key (str (:timestamp request))}
      [sim-request config request])]
   [:button.button.button-error.pure-button
    {:disabled (empty? requests)}
    "Clear Requests"]
   [:button.button.button-error.pure-button
    "Delete Simulator"]])
