(ns com.ben-allred.app-simulator.ui.utils.macros)

(defn set-timeout [f ms]
  #?(:cljs (.setTimeout js/window f ms)))

(defmacro after [ms & body]
  `(set-timeout (fn [] ~@body) ~ms))
