(ns com.ben-allred.clj-app-simulator.ui.services.events
  (:require
    [clojure.string :as string]
    [com.ben-allred.clj-app-simulator.utils.keywords :as keywords]
    [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(def code->key
  {13 :enter
   27 :esc})

(def key->code
  (let [code->key' (seq code->key)]
    (zipmap (map val code->key') (map key code->key'))))

(defn ->key-code [event]
  (let [code-ns (cond->> ()
                  (.-shiftKey event) (cons "shift")
                  (.-metaKey event) (cons "meta")
                  (.-ctrlKey event) (cons "ctrl")
                  (.-altKey event) (cons "alt")
                  :always (string/join "."))
        code (code->key (.-keyCode event))]
    (if (and code (seq code-ns))
      (keywords/join "/" [code-ns code])
      code)))
