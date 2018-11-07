(ns integration.utils.api
  (:refer-clojure :exclude [slurp])
  (:require [clojure.java.io :as io]))

(def ^:private with-prefix
  (partial str "test/fixtures/"))

(defn fixture->file [fixture]
  {:file (io/file (with-prefix fixture))
   :filename fixture
   :content-type "text/plain"})

(defn slurp [fixture]
  (clojure.core/slurp (with-prefix fixture)))
