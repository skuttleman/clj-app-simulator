(ns com.ben-allred.clj-app-simulator.ui.services.files-test
  (:require [cljs.test :as t :refer [deftest testing is]]
            [test.utils.spies :as spies]
            [cljs-http.client :as client]
            [com.ben-allred.clj-app-simulator.services.http :as http]
            [com.ben-allred.clj-app-simulator.ui.services.files :as files]))

(deftest ^:unit upload-test
  (testing "(upload)"
    (let [post-spy (spies/constantly ::post)
          request-spy (spies/constantly ::request)]
      (with-redefs [client/post post-spy
                    http/request* request-spy]
        (testing "uploads files"
          (let [result (files/upload ::url [::file-1 ::file-2])]
            (is (spies/called-with? post-spy ::url {:multipart-params [["files" ::file-1] ["files" ::file-2]]
                                                    :headers {"accept" "application/transit"}}))
            (is (spies/called-with? request-spy ::post))
            (is (= ::request result))))))))

(defn run-tests []
  (t/run-tests))
