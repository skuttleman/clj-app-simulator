(ns test.utils.dom
  (:refer-clojure :exclude [contains?])
  (:require
    [clojure.set :as set]
    [clojure.string :as string]
    [com.ben-allred.app-simulator.utils.keywords :as keywords]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [com.ben-allred.app-simulator.utils.maps :as maps]
    [com.ben-allred.app-simulator.utils.strings :as strings]))

(defn ^:private extract-classes
  ([attrs]
   (extract-classes attrs nil))
  ([attrs classes]
   (->> [(:class-name attrs) (:class attrs)]
        (concat (string/split (str classes) #"\."))
        (transduce (comp (map (comp strings/trim-to-nil keywords/safe-name))
                         (filter some?)
                         (mapcat (comp #(string/split % #"\s") keywords/safe-name)))
                   conj
                   #{}))))

(defn ^:private tag->map [tag & [attrs :as args]]
  (if (fn? tag)
    (-> attrs
        (select-keys #{:id})
        (maps/assoc-maybe :classes (extract-classes attrs))
        (merge {:component tag
                :args      args}))
    (let [[_ tag id classes] (re-find #"([^\#\.]+)?(\#[^\.]+)?(\..*)?" (name tag))
          classes (extract-classes attrs classes)]
      (cond-> {}
        (seq classes) (assoc :classes classes)
        tag (assoc :tag tag)
        id (assoc :id id)))))

(defn ^:private node-matches? [[tag attrs?] selector]
  (when (or (keyword? tag) (fn? tag))
    (let [tag-map (tag->map tag (when (map? attrs?) attrs?))
          {:keys [tag id classes component]} (tag->map selector)]
      (cond-> true
        tag (and (#{"*" (:tag tag-map)} tag))
        id (and (= id (:id tag-map)))
        classes (and (set/superset? (:classes tag-map) classes))
        component (and (= component (:component tag-map)))))))

(defn attrs [tree]
  (let [attrs (second tree)]
    (when (map? attrs)
      attrs)))

(defn children [[_ & more]]
  (cond-> more
    (map? (first more)) (rest)))

(defn query-all [tree selector]
  (let [matches (->> tree
                     (sequence (comp (filter sequential?)
                                     (mapcat (fn [node]
                                               (if ((some-fn keyword? fn?) (first node))
                                                 (query-all node selector)
                                                 (mapcat #(query-all % selector) node)))))))]
    (cond->> matches
      (and (sequential? tree) (node-matches? tree selector)) (cons tree))))

(defn query-one
  ([tree component selector]
   (query-one (into [:div] (query-all tree selector)) component))
  ([tree selector]
   (first (query-all tree selector))))

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
   (simulate-event tree event #?(:clj nil :cljs (js/Event. (name event)))))
  ([tree event event-data]
    #?(:clj  (throw (UnsupportedOperationException.))
       :cljs (when-let [f (get (attrs tree) (keywords/join "-" [:on event]))]
               (f event-data)))))

(defn contains? [tree item]
  (if (vector? item)
    (->> item
         (first)
         (query-all tree)
         (filter (comp (partial = (rest item)) rest))
         (seq)
         (boolean))
    (clojure.core/contains? (set (flatten tree)) item)))

(defn re-contains? [tree re]
  (->> tree
       (flatten)
       (filter string?)
       (string/join)
       (re-find re)))
