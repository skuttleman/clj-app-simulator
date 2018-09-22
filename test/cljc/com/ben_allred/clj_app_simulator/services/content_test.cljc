(ns com.ben-allred.clj-app-simulator.services.content-test
  (:require [clojure.test :as t :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.services.content :as content]))

(def ^:private header-keys #{"content-type" "accept"})

(deftest ^:unit parse-test
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

(deftest ^:unit prepare-test
  (testing "(prepare)"
    (testing "when preparing edn"
      (let [data {:headers {:some :header} :body #{:some :edn}}]
        (testing "stringifies the body"
          (is (= "#{:some :edn}" (:body (content/prepare data header-keys "application/edn")))))

        (testing "adds headers"
          (is (= {:some :header "content-type" "application/edn" "accept" "application/edn"}
                 (:headers (content/prepare data header-keys "application/edn")))))))

    (testing "when preparing json"
      (let [data {:headers {:some :header} :body {:some "json"}}]
        (testing "stringifies the body"
          (is (= "{\"some\":\"json\"}" (:body (content/prepare data header-keys "application/json")))))

        (testing "adds headers"
          (is (= {:some :header "content-type" "application/json" "accept" "application/json"}
                 (:headers (content/prepare data header-keys "application/json")))))))

    (testing "when preparing transit"
      (let [data {:headers {:some :header} :body {:some [:transit]}}]
        (testing "stringifies the body"
          (is (= "[\"^ \",\"~:some\",[\"~:transit\"]]" (:body (content/prepare data header-keys "application/transit")))))

        (testing "adds headers"
          (is (= {:some :header "content-type" "application/transit" "accept" "application/transit"}
                 (:headers (content/prepare data header-keys "application/transit")))))))

    (testing "does not update body when nil"
      (let [data {:nobody :here}]
        (is (nil? (:body (content/prepare data header-keys "application/edn"))))
        (is (nil? (:body (content/prepare data header-keys "application/json"))))
        (is (nil? (:body (content/prepare data header-keys "application/transit"))))))

    (testing "leaves string bodies untouched"
      (let [data {:body "a string"}]
        (is (= "a string" (:body (content/prepare data header-keys "application/edn"))))
        (is (= "a string" (:body (content/prepare data header-keys "application/json"))))
        (is (= "a string" (:body (content/prepare data header-keys "application/transit"))))))

    (testing "when the content-type is nil"
      (testing "stringifies the body as JSON"
        (is (= "{\"some\":\"data\"}" (:body (content/prepare {:body {:some :data}} header-keys nil))))))))

(defn run-tests []
  (t/run-tests))
