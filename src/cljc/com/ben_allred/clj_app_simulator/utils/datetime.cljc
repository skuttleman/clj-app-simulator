(ns com.ben-allred.clj-app-simulator.utils.datetime
  (:refer-clojure :exclude [format])
  #?(:clj  (:import (java.time Instant ZoneId)
                    (java.time.format DateTimeFormatter)
                    (java.util Date))
     :cljs (:require [cljsjs.moment])))

(def ^:private formatter
  #?(:clj  (.withZone (DateTimeFormatter/ofPattern "EEE MMM d, uuuu 'at' h:mm:ss a") (ZoneId/systemDefault))
     :cljs "ddd MMM D, YYYY [at] h:mm:ss A"))

(defn ^:private from-date [d]
  #?(:clj  (if (instance? Date d)
             (.toInstant d)
             d)
     :cljs (js/moment d)))

(defn ^:private from-string [s]
  #?(:clj  (Instant/parse s)
     :cljs (js/moment s)))

(defn ^:private from-millis [n]
  #?(:clj  (Instant/ofEpochMilli n)
     :cljs (js/moment n)))

(defn ^:private as-date [v]
  (cond-> v
    (string? v) (from-string)
    (integer? v) (from-millis)
    (inst? v) (from-date)))

(defn now []
  #?(:clj  (Instant/now)
     :cljs (js/moment)))

(defn format [dt]
  #?(:clj  (.format formatter (as-date dt))
     :cljs (.format (as-date dt) formatter)))
