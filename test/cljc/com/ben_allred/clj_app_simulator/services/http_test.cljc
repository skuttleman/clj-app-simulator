(ns com.ben-allred.clj-app-simulator.services.http-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.services.http :as http]
            [test.utils.spies :as spies]
            [kvlt.chan :as kvlt]
            [clojure.core.async :as async]))

(defn ^:private request* [method url request response]
  (let [response-ch (async/chan)
        kvlt-spy (spies/create (constantly response-ch))
        request (update request :headers assoc :content-type "application/edn")]
    (with-redefs [kvlt/request! kvlt-spy]
      (async/put! response-ch response)
      [(async/<!! (http/request* method url request)) kvlt-spy])))

(deftest ^:unit request*-test
  (testing "(request*)"
    (testing "when sending an http request"
      (testing "and when the request succeeds"
        (let [success-response {:status  201
                                :headers {:content-type "application/edn"}
                                :body    "{:some #{:edn :data}}"}
              [response spy] (request* ::method ::url {:some :request} success-response)]
          (testing "sends request data"
            (is (spies/called-with? spy {:method  ::method
                                         :url     ::url
                                         :headers {:content-type "application/edn" :accept "application/edn"}
                                         :some    :request})))
          (testing "places the data on the returned channel"
            (is (= [:success {:some #{:edn :data}} :created success-response] response)))))
      (testing "and when the request fails"
        (let [err {:status  404
                   :headers {:content-type "application/edn"}
                   :body    "{:some #{:edn :data}}"}
              error-response (ex-info "Some error" err)
              [response spy] (request* ::method ::url {:some :request} error-response)]
          (testing "sends request data"
            (is (spies/called-with? spy {:method  ::method
                                         :url     ::url
                                         :headers {:content-type "application/edn" :accept "application/edn"}
                                         :some    :request})))
          (testing "places the data on the returned channel"
            (is (= [:error {:some #{:edn :data}} :not-found err] response))))))))

(deftest ^:unit get-test
  (testing "(get)"
    (let [spy (spies/create (constantly ::response-channel))]
      (with-redefs [http/request* spy]
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
    (let [spy (spies/create (constantly ::response-channel))]
      (with-redefs [http/request* spy]
        (testing "sends a :post request"
          (http/post ::some-url ::some-request)
          (is (spies/called-with? spy :post ::some-url ::some-request)))))))

(deftest ^:unit patch-test
  (testing "(patch)"
    (let [spy (spies/create (constantly ::response-channel))]
      (with-redefs [http/request* spy]
        (testing "sends a :patch request"
          (http/patch ::some-url ::some-request)
          (is (spies/called-with? spy :patch ::some-url ::some-request)))))))

(deftest ^:unit put-test
  (testing "(put)"
    (let [spy (spies/create (constantly ::response-channel))]
      (with-redefs [http/request* spy]
        (testing "sends a :put request"
          (http/put ::some-url ::some-request)
          (is (spies/called-with? spy :put ::some-url ::some-request)))))))

(deftest ^:unit delete-test
  (testing "(delete)"
    (let [spy (spies/create (constantly ::response-channel))]
      (with-redefs [http/request* spy]
        (testing "sends a :delete request"
          (http/delete ::some-url ::some-request)
          (is (spies/called-with? spy :delete ::some-url ::some-request)))))))
