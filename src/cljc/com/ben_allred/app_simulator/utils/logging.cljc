(ns com.ben-allred.app-simulator.utils.logging
  (:require
    [clojure.string :as string]
    [com.ben-allred.app-simulator.utils.colors :as colors]
    [kvlt.core :refer [quiet!]]
    [taoensso.timbre :as logger #?@(:cljs [:include-macros true])]))

(defmacro debug [& args]
  `(logger/debug ~@args))

(defmacro info [& args]
  `(logger/info ~@args))

(defmacro warn [& args]
  `(logger/warn ~@args))

(defmacro error [& args]
  `(logger/error ~@args))

(defmacro spy* [expr result f spacer]
  `(let [result# ~result]
     (warn ~expr ~spacer (colors/colorize (~f result#)))
     result#))

(defmacro spy-tap [f expr]
  `(spy* (quote ~expr) ~expr ~f "\uD83C\uDF7A "))

(defmacro spy-on [f]
  `(fn [& args#]
     (spy* (cons (quote ~f) args#) (apply ~f args#) identity "\u27A1 ")))

(defmacro spy [expr]
  `(spy* (quote ~expr) ~expr identity "\uD83D\uDC40 "))

(defmacro tap-spy [expr f]
  `(spy* (quote ~expr) ~expr ~f "\uD83C\uDF7A "))

(defn ^:private ns-color [ns-str]
  (colors/with-style ns-str {:color :blue :trim? true}))

(defn ^:private level-color [level]
  (->> (case level
         :debug :cyan
         :warn :yellow
         :error :red
         :white)
       (assoc {:attribute :invert :trim? true} :color)
       (colors/with-style (str "[" (string/upper-case (name level)) "]"))))

(defn ^:private no-color [arg]
  (if-not (colors/colorized? arg)
    (colors/with-style arg {})
    arg))

(defn ^:private formatter [{:keys [level ?ns-str] :as data}]
  (update data :vargs (fn [vargs]
                        (conj #?(:clj  (seq vargs)
                                 :cljs (seq (map no-color vargs)))
                              (level-color level)
                              (ns-color (or ?ns-str "ns?"))))))

(quiet!)

(logger/merge-config!
  {:level      :debug
   :middleware [formatter]
   :appenders  {:println    {:enabled? false}
                :console    {:enabled? false}
                :system-out {:enabled? true
                             :fn       #?(:clj  #(.println System/out @(:msg_ %))
                                          :cljs #(apply (.-log js/console) (colors/prep-cljs (interpose (no-color "") (:vargs %)))))}}})
