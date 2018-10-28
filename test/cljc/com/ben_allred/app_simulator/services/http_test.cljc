(ns com.ben-allred.app-simulator.services.http-test
  (:require
    #?@(:clj  [[clojure.test :as t :refer [deftest is testing]]
               [test.utils.async :refer [async]]]
        :cljs [[cljs.test :as t :refer [async deftest is testing]]])
               [clojure.core.async :as async]
               [com.ben-allred.app-simulator.services.content :as content]
               [com.ben-allred.app-simulator.services.http :as http]
               [kvlt.chan :as kvlt]
               [test.utils.spies :as spies]))

(deftest ^:unit request*-test
  (testing "(request*)"
    (testing "when making a request"
      (async done
        (async/go
          (testing "and when the request succeeds"
            (let [response {:status  200
                            :headers {:content-type "application/edn"}
                            :body    "{:some (:edn)}"}
                  result (async/<! (http/request* (async/go response)))]
              (is (= [:success {:some [:edn]} :ok response]
                     result))))

          (testing "and when the request fails"
            (let [response {:status  500
                            :headers {:content-type "application/edn"}
                            :body    "{:errors #{:error-1 :error-2}}"}
                  result (async/<! (http/request* (async/go (ex-info "" response))))]
              (is (= [:error {:errors #{:error-1 :error-2}} :internal-server-error response]
                     result))))
          (done))))))

(deftest ^:unit go-test
  (testing "(go)"
    (let [content-spy (spies/constantly {:headers {:content-type "content-type" :accept "accept"}})
          kvlt-spy (spies/constantly ::kvlt)
          request-spy (spies/constantly ::request)]
      (with-redefs [content/prepare content-spy
                    kvlt/request! kvlt-spy
                    http/request* request-spy]
        (testing "makes the request"
          (let [result (http/go ::method ::url {::some ::request :headers {:accept ::accept}})]
            (is (spies/called-with? content-spy
                                    {::some   ::request
                                     :headers {:accept ::accept}
                                     :method  ::method
                                     :url     ::url}
                                    #{:content-type :accept}
                                    "application/transit"))
            (is (spies/called-with? kvlt-spy
                                    {:headers {:content-type "application/transit"
                                               :accept       ::accept}}))
            (is (spies/called-with? request-spy ::kvlt))
            (is (= ::request result))))))))

(deftest ^:unit get-test
  (testing "(get)"
    (let [spy (spies/constantly ::response-channel)]
      (with-redefs [http/go spy]
        (testing "sends a :get request"
          (spies/reset! spy)
          (is (= ::response-channel (http/get ::some-url)))
          (is (spies/called-with? spy :get ::some-url nil))
          (testing "with an optional request value"
            (spies/reset! spy)
            (http/get ::some-url ::some-request)
            (is (spies/called-with? spy :get ::some-url ::some-request))))))))

(deftest ^:unit post-test
  (testing "(post)"
    (let [spy (spies/constantly ::response-channel)]
      (with-redefs [http/go spy]
        (testing "sends a :post request"
          (http/post ::some-url ::some-request)
          (is (spies/called-with? spy :post ::some-url ::some-request)))))))

(deftest ^:unit patch-test
  (testing "(patch)"
    (let [spy (spies/constantly ::response-channel)]
      (with-redefs [http/go spy]
        (testing "sends a :patch request"
          (http/patch ::some-url ::some-request)
          (is (spies/called-with? spy :patch ::some-url ::some-request)))))))

(deftest ^:unit put-test
  (testing "(put)"
    (let [spy (spies/constantly ::response-channel)]
      (with-redefs [http/go spy]
        (testing "sends a :put request"
          (http/put ::some-url ::some-request)
          (is (spies/called-with? spy :put ::some-url ::some-request)))))))

(deftest ^:unit delete-test
  (testing "(delete)"
    (let [spy (spies/constantly ::response-channel)]
      (with-redefs [http/go spy]
        (testing "sends a :delete request"
          (http/delete ::some-url ::some-request)
          (is (spies/called-with? spy :delete ::some-url ::some-request)))))))

(defn run-tests []
  (t/run-tests))
