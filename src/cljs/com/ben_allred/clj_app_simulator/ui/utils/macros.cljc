(ns com.ben-allred.clj-app-simulator.ui.utils.macros)

(defmacro after [ms & body]
    `(.setTimeout js/window (fn [] ~@body) ~ms))
