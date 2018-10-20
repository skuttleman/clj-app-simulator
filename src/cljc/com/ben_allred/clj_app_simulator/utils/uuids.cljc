(ns com.ben-allred.clj-app-simulator.utils.uuids
  #?(:clj
     (:import
       (java.util UUID))))

(defn ->uuid [v]
  (when v
    (if (uuid? v)
      v
      #?(:clj  (UUID/fromString v)
         :cljs (uuid v)))))

(defn random []
  #?(:clj  (UUID/randomUUID)
     :cljs (random-uuid)))
