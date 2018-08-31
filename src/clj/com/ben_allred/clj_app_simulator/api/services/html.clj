(ns com.ben-allred.clj-app-simulator.api.services.html
  (:require [com.ben-allred.clj-app-simulator.templates.core :as templates]
            [com.ben-allred.clj-app-simulator.templates.views.core :as views]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.services.ui-reducers :as ui-reducers]
            [hiccup.core :as hiccup]))

(defn ^:private hiccup [tree]
  (hiccup/html tree))

(def ^:private path-for
  (constantly "#"))

(def ^:private app-attrs
  {:header     (partial views/header path-for)
   :components {:home      (constantly [views/root
                                        [[:div.button-row
                                          [:button.button.button-success.pure-button
                                           "Create"]]
                                         [views/spinner]]])
                :new       (partial views/new [views/spinner])
                :details   (constantly [views/details [views/spinner]])
                :resources (constantly [views/resources [views/spinner]])}
   :not-found  (partial views/not-found path-for)
   :toast      (constantly [:div.toast-container])
   :modal      (constantly [:div.modal-wrapper])})

(defn app [state]
  [views/app* app-attrs state])

(defn ^:private template [content]
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name :viewport :content "width=device-width, initial-scale=1"}]
    [:title "App Simulator"]
    [:link {:rel  "stylesheet"
            :href "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.6.3/css/font-awesome.min.css"
            :type "text/css"}]
    [:link {:rel         "stylesheet"
            :href        "https://unpkg.com/purecss@1.0.0/build/pure-min.css"
            :type        "text/css"
            :integrity   "sha384-nn4HPE8lTHyVtfCBi5yW9d20FjT8BJwUXyWZT9InLYax14RDjBj46LmSztkmNP9w"
            :crossorigin "anonymous"}]
    [:link {:rel  "stylesheet"
            :href "/css/main.css"
            :type "text/css"}]]
   [:body
    [:div#app content]
    [:script {:type "text/javascript" :src "/js/compiled/app.js"}]
    [:script
     {:type "text/javascript"}
     "com.ben_allred.clj_app_simulator.ui.app.mount_BANG_();"]]])

(defn tree->html [tree]
  (->> tree
       (hiccup)
       (str "<!DOCTYPE html>")))

(defn build-tree [content]
  (-> content
      (templates/render)
      (template)))

(defn hydrate [page]
  (->> (assoc (ui-reducers/root) :page page)
       (conj [app])
       (build-tree)
       (tree->html)))

(defn render [{:keys [uri params]}]
  (-> (condp re-matches uri
        #"/details/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})"
        :>> (fn [[_ id]]
              {:handler :details :route-params {:id id}})

        #"/resources"
        {:handler :resources}

        #"/create"
        {:handler :new :query-params (select-keys params #{:type})}

        #"/"
        {:handler :home}

        {:handler :not-found})
      (hydrate)))
