(ns com.ben-allred.clj-app-simulator.ui.simulators.http.transformations
  (:require [com.ben-allred.formation.core :as f]
            [com.ben-allred.clj-app-simulator.utils.strings :as strings]
            [com.ben-allred.clj-app-simulator.utils.fns :as fns :include-macros true]
            [clojure.string :as string]))

(defn ^:private maybe-parse-int [value]
  (let [delay (js/parseInt value)
        delay-str (str delay)
        trim-zeros (string/replace value #"^0+" "")]
    (if (or (= trim-zeros delay-str)
            (= (str 0 trim-zeros) delay-str))
      delay
      value)))

(def source->model
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

(def model->view
  {:delay    str
   :response {:status  str
              :headers (f/transformer-tuple
                         [name strings/titlize]
                         identity)}})

(def view->model
  {:delay    maybe-parse-int
   :response {:status  js/parseInt
              :headers (f/transformer-tuple
                         [string/lower-case keyword]
                         identity)}})

(def model->source
  (f/make-transformer
    {:response    {:headers #(reduce (fn [m [k v]]
                                       (let [existing (get m k)
                                             trimmed (strings/trim-to-nil v)]
                                         (cond
                                           (not trimmed) m
                                           (string? existing) (assoc m k [existing trimmed])
                                           (coll? existing) (update m k conj trimmed)
                                           :else (assoc m k trimmed))))
                                     {}
                                     %)
                   :body    strings/trim-to-nil}
     :name        strings/trim-to-nil
     :group       strings/trim-to-nil
     :description strings/trim-to-nil}))

(defn sim->model [sim]
  (-> sim
      (:config)
      (select-keys #{:group :response :delay :name :description})
      (source->model)))
