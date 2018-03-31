(ns com.ben-allred.clj-app-simulator.services.env
  (:refer-clojure :exclude [get])
  #?(:clj (:require [environ.core :as environ])))

(def get
  #?(:clj  environ/env
     :cljs {}))

(def dev?
  #?(:clj  (not= "production" (get :ring-env))
     :cljs (boolean (re-find #"localhost" (.-hostname (.-location js/window))))))
