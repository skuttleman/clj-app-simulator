(ns com.ben-allred.clj-app-simulator.utils.json
  #?(:clj (:require [jsonista.core :as jsonista]
                    [com.ben-allred.clj-app-simulator.utils.keywords :as keywords]
                    [com.ben-allred.clj-app-simulator.utils.logging :as log])))

(def ^:private mapper
  #?(:clj  (jsonista/object-mapper
             {:encode-key-fn keywords/safe-name
              :decode-key-fn keyword})
     :cljs nil))

(defn parse [s]
  #?(:clj  (jsonista/read-value s mapper)
     :cljs (js->clj (.parse js/JSON s) :keywordize true)))

(defn stringify [o]
  #?(:clj  (jsonista/write-value-as-string o)
     :cljs (.stringify js/JSON o)))
