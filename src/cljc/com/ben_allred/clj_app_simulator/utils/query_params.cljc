(ns com.ben-allred.clj-app-simulator.utils.query-params
  (:require [com.ben-allred.clj-app-simulator.utils.keywords :as keywords]
            [clojure.string :as string]))

(defn ^:private namify [[k v]]
  [(str (keywords/safe-name k)) (str (keywords/safe-name v))])

(defn ^:private parsify [[k v]]
  [(keyword k) (or v true)])

(defn parse [s]
  (into {}
        (comp (map #(string/split % #"="))
              (filter (comp seq first))
              (map parsify))
        (string/split (str s) #"&")))

(defn stringify [qp]
  (->> qp
       (map (comp (partial string/join "=") namify))
       (string/join "&")))
