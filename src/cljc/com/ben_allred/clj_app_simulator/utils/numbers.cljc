(ns com.ben-allred.clj-app-simulator.utils.numbers)

(defn nan? [v]
  #?(:clj  (try (Double/isNaN (double v))
                (catch Throwable ex
                  true))
     :cljs (js/isNaN v)))

(defn parse-int [v]
  #?(:clj  (try (Long/parseLong (str v))
                (catch Throwable ex
                  Double/NaN))
     :cljs (js/parseInt (str v))))
