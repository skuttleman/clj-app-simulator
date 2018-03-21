(ns com.ben-allred.clj-app-simulator.services.content-test
    (:require [com.ben-allred.clj-app-simulator.services.content :as content]
        #?(:clj [clojure.test :refer [deftest testing is]]
           :cljs [cljs.test :refer-macros [deftest testing is]])))

(deftest parse-test
    (testing "(parse)"
        (testing "leaves unknown content-type unparsed"
            (is (= {:some :data} (content/parse {:some :data} "unknown-content"))))))
