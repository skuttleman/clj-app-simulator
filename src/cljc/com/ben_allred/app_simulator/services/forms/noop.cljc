(ns com.ben-allred.app-simulator.services.forms.noop
  (:require
    [com.ben-allred.app-simulator.services.forms.core :as forms])
  #?(:clj
     (:import
       (clojure.lang IAtom IDeref))))

(defn create [model]
  (reify
    forms/ISync
    (ready! [_])
    (ready! [_ _ _])
    (sync! [_])
    (syncing? [_] false)

    forms/IChange
    (touch! [_ _])
    (changed? [_] false)
    (changed? [_ _] false)
    (touched? [_] false)
    (touched? [_ _] false)


    forms/IValidate
    (errors [_] nil)
    (valid? [_] true)

    forms/ITry
    (try! [_])
    (tried? [_] false)

    #?@(:clj  [IAtom
               (reset [_ _])
               (swap [_ _])
               (swap [_ _ _])
               (swap [_ _ _ _])
               (swap [_ _ _ _ _])
               IDeref
               (deref [_]
                 model)]
        :cljs [IReset
               (-reset! [_ _])
               ISwap
               (-swap! [_ _])
               (-swap! [_ _ _])
               (-swap! [_ _ _ _])
               (-swap! [_ _ _ _ _])
               IDeref
               (-deref [_]
                 model)])))
