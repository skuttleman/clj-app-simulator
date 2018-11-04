(ns com.ben-allred.app-simulator.templates.resources.file-test
  (:require
    [clojure.test :as t :refer [deftest is testing]]
    [test.utils.spies :as spies]
    [com.ben-allred.app-simulator.templates.resources.file :as resources]
    [com.ben-allred.formation.core :as f]))

(defn ^:private pred [_ msg]
  (condp re-find msg
    #"whole" ::whole
    #"negative" ::positive
    #"number" ::number
    #"(?i)header.+key" ::header-key
    #"(?i)content.type.+file" ::no-content-type
    #"(?i)header.+value" ::header-value
    #"(?i)path" ::path
    nil))

(deftest ^:unit validate-existing*-test
  (testing "(validate-existing*)"
    (with-redefs [f/pred (spies/create pred)
                  f/required (spies/constantly ::required)
                  f/validator-coll (spies/constantly ::coll)
                  f/validator-tuple (spies/constantly ::tuple)
                  f/make-validator (spies/constantly ::validator)]
      (let [validator (resources/validate-existing*)
            validator-map (ffirst (spies/calls f/make-validator))]
        (testing "makes a validator"
          (is (= ::validator validator))
          (is (spies/called-times? f/pred 6)))

        (testing "validates :delay"
          (let [delay (:delay validator-map)]
            (is (= #{::number ::whole ::positive} (set delay)))))

        (testing "when validating :response :status"
          (is (spies/called? f/required))
          (is (= ::required (get-in validator-map [:response :status]))))

        (testing "when validating :response :headers"
          (is (spies/called-with? f/validator-tuple [::header-key ::no-content-type] ::header-value))
          (is (spies/called-with? f/validator-coll ::tuple))
          (is (= ::coll (get-in validator-map [:response :headers]))))))))

(deftest ^:unit validate-new*-test
  (testing "(validate-new*)"
    (with-redefs [f/pred (spies/create pred)
                  f/required (spies/constantly ::required)
                  f/validator-coll (spies/constantly ::coll)
                  f/validator-tuple (spies/constantly ::tuple)
                  f/make-validator (spies/constantly ::validator)]
      (let [validator (resources/validate-new*)
            validator-map (ffirst (spies/calls f/make-validator))]
        (testing "makes a validator"
          (is (= ::validator validator))
          (is (spies/called-times? f/pred 7)))

        (testing "validates :path"
          (let [path (:path validator-map)]
            (is (spies/called? f/required))
            (is (= #{::path ::required} (set path)))))

        (testing "validates :method"
          (let [method (:method validator-map)]
            (is (= ::required method))))

        (testing "validates :delay"
          (let [delay (:delay validator-map)]
            (is (= #{::number ::whole ::positive} (set delay)))))

        (testing "when validating :response :status"
          (is (spies/called? f/required))
          (is (= ::required (get-in validator-map [:response :status]))))

        (testing "when validating :response :headers"
          (is (spies/called-with? f/validator-tuple [::header-key ::no-content-type] ::header-value))
          (is (spies/called-with? f/validator-coll ::tuple))
          (is (= ::coll (get-in validator-map [:response :headers]))))))))

(defn run-tests []
  (t/run-tests))
