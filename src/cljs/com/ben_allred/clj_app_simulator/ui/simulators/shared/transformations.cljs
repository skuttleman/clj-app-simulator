(ns com.ben-allred.clj-app-simulator.ui.simulators.shared.transformations
  (:require [clojure.string :as string]
            [com.ben-allred.formation.core :as f]
            [com.ben-allred.clj-app-simulator.utils.strings :as strings]))

(defn ^:private maybe-parse-int [value]
  (let [delay (js/parseInt value)
        delay-str (str delay)
        trim-zeros (string/replace value #"^0+" "")]
    (if (or (= trim-zeros delay-str)
            (= (str 0 trim-zeros) delay-str))
      delay
      value)))

(def model->view
  {:path     str
   :method   (comp #(subs % 1) str)
   :delay    str
   :response {:status  str
              :headers (f/transformer-tuple
                         [name strings/titlize]
                         identity)}})

(def view->model
  {:path     strings/trim-to-nil
   :method   keyword
   :delay    maybe-parse-int
   :response {:status  js/parseInt
              :headers (f/transformer-tuple
                         [string/lower-case keyword]
                         identity)}})

(def headers->source
  {:headers #(reduce (fn [m [k v]]
                       (let [existing (get m k)
                             trimmed (strings/trim-to-nil v)]
                         (cond
                           (not trimmed) m
                           (string? existing) (assoc m k [existing trimmed])
                           (coll? existing) (update m k conj trimmed)
                           :else (assoc m k trimmed))))
                     {}
                     %)})

(def model->source
  {:name        strings/trim-to-nil
   :group       strings/trim-to-nil
   :description strings/trim-to-nil})
