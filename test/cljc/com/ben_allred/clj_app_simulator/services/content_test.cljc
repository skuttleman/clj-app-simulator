(ns com.ben-allred.clj-app-simulator.services.content-test
    (:require [clojure.test :refer [deftest testing is]]
              [com.ben-allred.clj-app-simulator.services.content :as content]))

(deftest parse-test
    (testing "(parse)"
        (testing "parses edn"
            (let [data {:body "#{:some :edn}"}]
                (is (= #{:some :edn} (:body (content/parse data "application/edn"))))))
        (testing "parses json"
            (let [data {:body "{\"some\":\"json\"}"}]
                (is (= {:some "json"} (:body (content/parse data "application/json"))))))
        (testing "parses transit"
            (let [data {:body "[\"^ \",\"~:some\",[\"~:transit\"]]"}]
                (is (= {:some [:transit]} (:body (content/parse data "application/transit"))))))
        (testing "does not update body when nil"
            (let [data {:nobody :here}]
                (is (nil? (:body (content/parse data "application/edn"))))
                (is (nil? (:body (content/parse data "application/json"))))
                (is (nil? (:body (content/parse data "application/transit"))))))
        (testing "leaves unknown content-type unparsed"
            (is (= :data (:body (content/parse {:body :data} "unknown-content")))))))

(deftest prepare-test
    (testing "(prepare)"
        (testing "when preparing edn"
            (let [data {:headers {:some :header} :body #{:some :edn}}]
                (testing "stringifies the body"
                    (is (= "#{:some :edn}" (:body (content/prepare data "application/edn")))))
                (testing "adds headers"
                    (is (= {:some :header "content-type" "application/edn" "accept" "application/edn"}
                           (:headers (content/prepare data "application/edn")))))))
        (testing "when preparing json"
            (let [data {:headers {:some :header} :body {:some "json"}}]
                (testing "stringifies the body"
                    (is (= "{\"some\":\"json\"}" (:body (content/prepare data "application/json")))))
                (testing "adds headers"
                    (is (= {:some :header "content-type" "application/json" "accept" "application/json"}
                           (:headers (content/prepare data "application/json")))))))
        (testing "when preparing transit"
            (let [data {:headers {:some :header} :body {:some [:transit]}}]
                (testing "stringifies the body"
                    (is (= "[\"^ \",\"~:some\",[\"~:transit\"]]" (:body (content/prepare data "application/transit")))))
                (testing "adds headers"
                    (is (= {:some :header "content-type" "application/transit" "accept" "application/transit"}
                           (:headers (content/prepare data "application/transit")))))))
        (testing "does not update body when nil"
            (let [data {:nobody :here}]
                (is (nil? (:body (content/prepare data "application/edn"))))
                (is (nil? (:body (content/prepare data "application/json"))))
                (is (nil? (:body (content/prepare data "application/transit"))))))
        (testing "leaves string bodies untouched"
            (let [data {:some :data :body "a string"}]
                (is (= data (content/prepare data "application/edn")))
                (is (= data (content/prepare data "application/json")))
                (is (= data (content/prepare data "application/transit")))))
        (testing "leaves unknown content-type untouched"
            (is (= {:some :data} (:body (content/prepare {:body {:some :data}} "unknown-content")))))))