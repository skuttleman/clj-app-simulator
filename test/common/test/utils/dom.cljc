(ns test.utils.dom
  (:refer-clojure :exclude [contains?])
  (:require [clojure.string :as string]
            [clojure.set :as set]
            [com.ben-allred.clj-app-simulator.utils.keywords :as keywords]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(defn ^:private tag->map [tag & [attrs :as args]]
  (if (fn? tag)
    {:component tag
     :args      args}
    (let [[_ tag id classes] (re-find #"([^\#\.]+)?(\#[^\.]+)?(\..*)?" (name tag))
          class-name (->> [(:class attrs) (:class-name attrs)]
                          (mapcat #(string/split (str (keywords/safe-name %)) #"\s"))
                          (string/join "."))
          classes (->> (string/split (str classes "." class-name) #"\.")
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
             (and (sequential? tree) (node-matches? tree selector)) (cons tree))))

(defn query-one [tree selector]
  (first (query-all tree selector)))

(defn render [[elem & args :as tree]]
  (if (fn? elem)
    (apply elem args)
    (mapv (fn [node]
            (cond
              (vector? node) (render node)
              (list? node) (map render node)
              :else node))
          tree)))

(defn simulate-event
  ([tree event]
   (simulate-event tree event #?(:clj (Object.) :cljs (js/Event. (name event)))))
  ([tree event event-data]
   (when-let [f (get (attrs tree) (keywords/join "-" [:on event]))]
     (f event-data))))

(defn contains? [tree item]
  (if (vector? item)
    (->> item
         (first)
         (query-all tree)
         (filter (comp (partial = (rest item)) rest))
         (seq)
         (boolean))
    (clojure.core/contains? (set (flatten tree)) item)))
