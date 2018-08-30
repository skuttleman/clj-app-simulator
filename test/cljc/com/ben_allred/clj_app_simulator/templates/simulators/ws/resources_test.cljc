(ns com.ben-allred.clj-app-simulator.templates.simulators.ws.resources-test
  (:require [clojure.test :refer [deftest testing is are]]
            [test.utils.spies :as spies]
            [com.ben-allred.formation.core :as f]
            [com.ben-allred.clj-app-simulator.templates.simulators.ws.resources :as resources]))

(deftest ^:unit validate-new*-test
  (testing "(validate-new*)"
    (let [make-spy (spies/constantly ::validator)
          required-spy (spies/constantly ::required)
          pred-spy (spies/constantly ::pred)]
      (with-redefs [f/make-validator make-spy
                    f/required required-spy
                    f/pred pred-spy]
        (let [validator (resources/validate-new*)]
          (testing "constructs a validator"
            (is (spies/called-times? required-spy 2))
            (is (spies/called? pred-spy))
            (is (-> make-spy
                    (spies/calls)
                    (ffirst)
                    (update :path set)
                    (= {:path #{::required ::pred} :method ::required})))
            (is (= ::validator validator)))

          (testing "when validating the path"
            (let [path-pred (ffirst (spies/calls pred-spy))]
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
                     "/$$$")))))))))

(deftest ^:unit socket-message*-test
  (testing "(socket-message*)"
    (let [validator-spy (spies/constantly ::validator)
          pred-spy (spies/constantly ::required)]
      (with-redefs [f/make-validator validator-spy
                    f/pred pred-spy]
        (let [socket-message (resources/socket-message*)]
          (is (spies/called-with? pred-spy seq (spies/matcher string?)))
          (is (spies/called-with? validator-spy {:message ::required}))
          (is (= ::validator socket-message)))))))
