(ns test.utils.date-time
    #?(:clj
       (:import [java.util Date])))

(defn ^:private abs [value]
    #?(:clj  (Math/abs value)
       :cljs (.abs js/Math value)))

(defn date-within
    ([date-1 tolerance]
     (date-within date-1 #?(:clj (Date.) :cljs (js/Date.)) tolerance))
    ([date-1 date-2 tolerance]
     (> tolerance (abs (- (.getTime date-1) (.getTime date-2))))))
