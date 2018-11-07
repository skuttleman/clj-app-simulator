(ns com.ben-allred.app-simulator.api.core
  (:import
    (clojure.lang ExceptionInfo)))

(defmacro de-http [& body]
  `(try
     ~@body
     (catch ExceptionInfo ex#
       (let [data# (ex-data ex#)]
         (throw (ex-info (get-in data# [:response :body :message] "Something went wrong")
                         (dissoc data# :response :type)))))))
