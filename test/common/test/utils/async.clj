(ns test.utils.async
  (:require [clojure.core.async :as async]))

(defmacro async [cb & body]
  `(let [~cb (constantly nil)]
     (async/<!! ~@body)))
