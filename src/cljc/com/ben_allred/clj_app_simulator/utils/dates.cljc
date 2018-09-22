(ns com.ben-allred.clj-app-simulator.utils.dates
  (:refer-clojure :exclude [format])
  #?(:clj
           (:import (java.time Instant ZoneId)
                    (java.util Date)
                    (java.time.format DateTimeFormatter))
     :cljs (:require [cljsjs.moment])))

(def ^:private default-format
  #?(:clj  "EEE MMM d, YYYY 'at' h:mm:ss a z"
     :cljs "ddd MMM D, YYYY [at] h:mm:ss a z"))

(defn ^:private inst->dt [inst]
  #?(:clj  (if (instance? Instant inst)
             inst
             (.toInstant inst))
     :cljs (if (.isMoment js/moment inst)
             inst
             (js/moment inst))))

(defn format
  ([inst]
   (format inst default-format))
  ([inst fmt]
    #?(:clj  (-> fmt
                 (DateTimeFormatter/ofPattern)
                 (.withZone (ZoneId/of "UTC"))
                 (.format (inst->dt inst)))
       :cljs (.format (inst->dt inst) fmt))))
