(ns test.utils.dom
    (:refer-clojure :exclude [contains?])
    (:require [clojure.string :as string]
              [clojure.set :as set]
              [com.ben-allred.clj-app-simulator.utils.keywords :as keywords]
              [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(defn ^:private tag->map [tag & [attrs :as args]]
    (if (fn? tag)
        {:component tag
         :args args}
        (let [[_ tag id classes] (re-find #"([^\#\.]+)?(\#[^\.]+)?(\..*)?" (name tag))
              class-name (->> (string/split (str (:class-name attrs)) #"\s")
                              (string/join "."))
              classes    (->> (string/split (str classes "." class-name) #"\.")
                              (map string/trim)
                              (filter seq)
                              (set))]
            (cond-> {}
                (seq classes) (assoc :classes classes)
                tag (assoc :tag tag)
                id (assoc :id id)))))

(defn ^:private node-matches? [[tag attrs?] selector]
    (let [tag-map (tag->map tag (when (map? attrs?) attrs?))
          {:keys [tag id classes component]} (tag->map selector)]
        (cond-> true
            tag (and (#{"*" (:tag tag-map)} tag))
            id (and (= id (:id tag-map)))
            classes (and (set/superset? (:classes tag-map) classes))
            component (and (= component (:component tag-map))))))

(defn attrs [tree]
    (let [attrs (second tree)]
        (when (map? attrs)
            attrs)))

(defn query-all [tree selector]
    (let [matches (->> tree
                       (filter sequential?)
                       (mapcat (fn [node]
                                   (if ((some-fn keyword? fn?) (first node))
                                       (query-all node selector)
                                       (mapcat #(query-all % selector) node)))))]
        (cond->> matches
            (node-matches? tree selector) (cons tree))))

(defn query-one [tree selector]
    (first (query-all tree selector)))

(defn simulate-event [tree event & [event-data]]
    (when-let [f (get (attrs tree) (keywords/join "-" [:on event]))]
        (f (or event-data (js/Event. (name event))))))

(defn contains? [tree item]
    (clojure.core/contains? (set (flatten tree)) item))