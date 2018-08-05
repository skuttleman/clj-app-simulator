(ns com.ben-allred.clj-app-simulator.ui.simulators.http.resources
  (:require [clojure.string :as string]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.resources :as shared.resources]
            [com.ben-allred.formation.core :as f]))

(def statuses shared.resources/statuses)

(def http-methods
  (->> [:http/get :http/post :http/put :http/patch :http/delete]
       (map (juxt str (comp string/upper-case name)))))

(defn edit-config []
  {:delay    [(f/pred #(and (not (js/isNaN %)) (number? %)) "Delay must be a number")
              (f/pred #(= (js/parseInt %) %) "Delay must be a whole number")
              (f/pred #(>= % 0) "Delay cannot be negative")]
   :response {:status  (f/required "Must have a status")
              :headers (f/validator-coll
                         (f/validator-tuple
                           (f/pred (comp (partial re-matches #"[A-Za-z0-9_-]+") name) "Invalid header key")
                           (f/pred #(seq %) "Header must have a value")))}})

(defn new-config []
  {:path     [(f/required "Must have a path")
              (f/pred #(or (= % "/") (re-matches #"(/:?[A-Za-z-_0-9]+)+" %)) "Invalid path")]
   :method   (f/required "Must have a method")})

(defn validate-existing* []
  (f/make-validator (edit-config)))

(defn validate-new* []
  (f/make-validator (merge (edit-config) (new-config))))

(def validate-existing (validate-existing*))

(def validate-new (validate-new*))
