(ns com.ben-allred.app-simulator.templates.resources.ws-test
  (:require
    [clojure.test :as t :refer [are deftest is testing]]
    [com.ben-allred.app-simulator.templates.resources.ws :as resources]
    [com.ben-allred.formation.core :as f]
    [test.utils.spies :as spies]))

(deftest ^:unit validate-new*-test
  (testing "(validate-new*)"
    (with-redefs [f/make-validator (spies/constantly ::validator)
                  f/required (spies/constantly ::required)
                  f/pred (spies/constantly ::pred)]
      (let [validator (resources/validate-new*)]
        (testing "constructs a validator"
          (is (spies/called-times? f/required 2))
          (is (spies/called? f/pred))
          (is (-> f/make-validator
                  (spies/calls)
                  (ffirst)
                  (update :path set)
                  (= {:path #{::required ::pred} :method ::required})))
          (is (= ::validator validator)))

        (testing "when validating the path"
          (let [path-pred (ffirst (spies/calls f/pred))]
            (testing "recognizes valid paths"
              (are [path] (path-pred path)
                "/"
                "/some"
                "/:some/path"
                "/this/:is/_also/valid-123"))

            (testing "recognizes invalid paths"
              (are [path] (not (path-pred path))
                ""
                "\\"
                ":something"
                "/$$$"))))))))

(deftest ^:unit socket-message*-test
  (testing "(socket-message*)"
    (with-redefs [f/make-validator (spies/constantly ::validator)
                  f/required (spies/constantly ::required)]
      (let [socket-message (resources/socket-message*)]
        (is (spies/called-with? f/required (spies/matcher string?)))
        (is (spies/called-with? f/make-validator {:message ::required}))
        (is (= ::validator socket-message))))))

(defn run-tests []
  (t/run-tests))
