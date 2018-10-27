(ns com.ben-allred.clj-app-simulator.utils.dates
  (:refer-clojure :exclude [format])
  #?(:clj
     (:import
       (java.time Instant ZonedDateTime ZoneId ZoneOffset)
       (java.time.chrono ChronoLocalDateTime ChronoZonedDateTime)
       (java.time.format DateTimeFormatter DateTimeParseException)
       (java.util Date))
     :cljs
     (:require
       [cljsjs.moment])))

(def ^:private default-format
  #?(:clj  "EEE MMM d, YYYY 'at' h:mm a"
     :cljs "ddd MMM D, YYYY [at] h:mm a"))

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
                 (.withZone (ZoneId/systemDefault))
                 (.format (inst->dt inst)))
       :cljs (.format (inst->dt inst) fmt))))

(defn inst-str? [s]
  (boolean (and (string? s)
                (re-matches #"\d{4}-[0-1][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\.\d{3}(Z|(\+|-)\d{2}:?\d{2})" s)
                #?(:clj  (try
                           (ZonedDateTime/parse s)
                           (catch DateTimeParseException ex
                             false))
                   :cljs (.isValid (js/moment s))))))

(defn ->inst [v]
  (cond
    (inst? v)
    v
    #?(:clj (instance? Instant v))
    #?(:clj (Date/from v))

    #?(:clj (instance? ChronoZonedDateTime v))
    #?(:clj (->inst (.toInstant v)))

    #?(:clj (instance? ChronoLocalDateTime v))
    #?(:clj (->inst (.toInstant v ZoneOffset/UTC)))

    #?(:cljs (and (.isMoment js/moment v)
                  (.isValid v)))
    #?(:cljs (.toDate v))

    (inst-str? v)
    #?(:clj  (->inst (ZonedDateTime/parse v))
       :cljs (->inst (js/moment v)))))
