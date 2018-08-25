(ns com.ben-allred.clj-app-simulator.api.services.html
  (:require [com.ben-allred.clj-app-simulator.templates.core :as templates]
            [hiccup.core :as hiccup]))

(defn ^:private template [content]
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name :viewport :content "width=device-width, initial-scale=1"}]
    [:title "App Simulator"]
    [:link {:rel "stylesheet" :href "/css/main.css" :type "text/css"}]
    [:link {:rel "stylesheet" :href "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.6.3/css/font-awesome.min.css" :type :text/css}]
    [:link {:rel "stylesheet" :href "https://unpkg.com/purecss@1.0.0/build/pure-min.css" :integrity "sha384-nn4HPE8lTHyVtfCBi5yW9d20FjT8BJwUXyWZT9InLYax14RDjBj46LmSztkmNP9w" :crossorigin "anonymous"}]]
   [:body
    [:div#app content]
    [:script {:type "text/javascript" :src "/js/compiled/app.js"}]
    [:script
     {:type "text/javascript"}
     "com.ben_allred.clj_app_simulator.ui.app.mount_BANG_();"]]])

(defn index
  ([] (index nil))
  ([content]
   (-> content
       (templates/render)
       (template)
       (hiccup/html))))
