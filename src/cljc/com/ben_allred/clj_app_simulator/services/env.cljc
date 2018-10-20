(ns com.ben-allred.clj-app-simulator.services.env
  (:refer-clojure :exclude [get])
  #?(:clj
     (:require
       [environ.core :as environ])))

(def get
  #?(:clj  environ/env
     :cljs {:host     (.-host (.-location js/window))
            :protocol (if (re-find #"https" (.-protocol (.-location js/window)))
                        :https
                        :http)}))

(def dev?
  #?(:clj  (not= "production" (get :ring-env))
     :cljs (boolean (re-find #"localhost" (get :host)))))
