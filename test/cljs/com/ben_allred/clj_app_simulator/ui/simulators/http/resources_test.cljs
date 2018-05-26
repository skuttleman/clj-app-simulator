(ns com.ben-allred.clj-app-simulator.ui.simulators.http.resources-test
  (:require [cljs.test :as t :refer [deftest testing is]]
            [test.utils.spies :as spies]
            [com.ben-allred.formation.core :as f]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.resources :as resources]))

(defn ^:private pred [_ msg]
  (condp re-find msg
      #"whole" ::whole
      #"negative" ::positive
      #"number" ::number
      #"(?i)header.+key" ::header-key
      #"(?i)header.+value" ::header-value
      #"(?i)path" ::path
      nil))

(deftest ^:unit validate-existing*-test
  (testing "(validate-existing*)"
      (let [pred-spy (spies/create pred)
            required-spy (spies/constantly ::required)
            coll-spy (spies/constantly ::coll)
            tuple-spy (spies/constantly ::tuple)
            validator-spy (spies/constantly ::validator)]
        (with-redefs [f/pred pred-spy
                      f/required required-spy
                      f/validator-coll coll-spy
                      f/validator-tuple tuple-spy
                      f/make-validator validator-spy]
          (let [validator (resources/validate-existing*)
                validator-map (ffirst (spies/calls validator-spy))]
            (testing "makes a validator"
              (is (= ::validator validator))
              (is (spies/called-times? pred-spy 5)))

            (testing "validates :delay"
              (let [delay (:delay validator-map)]
                (is (= #{::number ::whole ::positive} (set delay)))))

            (testing "when validating :response :status"
              (is (spies/called? required-spy))
              (is (= ::required (get-in validator-map [:response :status]))))

            (testing "when validating :response :headers"
              (is (spies/called-with? tuple-spy ::header-key ::header-value))
              (is (spies/called-with? coll-spy ::tuple))
              (is (= ::coll (get-in validator-map [:response :headers])))))))))

(deftest ^:unit validate-new*-test
  (testing "(validate-new*)"
    (let [pred-spy (spies/create pred)
          required-spy (spies/constantly ::required)
          coll-spy (spies/constantly ::coll)
          tuple-spy (spies/constantly ::tuple)
          validator-spy (spies/constantly ::validator)]
      (with-redefs [f/pred pred-spy
                    f/required required-spy
                    f/validator-coll coll-spy
                    f/validator-tuple tuple-spy
                    f/make-validator validator-spy]
        (let [validator (resources/validate-new*)
              validator-map (ffirst (spies/calls validator-spy))]
          (testing "makes a validator"
            (is (= ::validator validator))
            (is (spies/called-times? pred-spy 6)))

          (testing "validates :path"
            (let [path (:path validator-map)]
              (is (spies/called? required-spy))
              (is (= #{::path ::required} (set path)))))

          (testing "validates :method"
            (let [method (:method validator-map)]
              (is (= ::required method))))

          (testing "validates :delay"
            (let [delay (:delay validator-map)]
              (is (= #{::number ::whole ::positive} (set delay)))))

          (testing "when validating :response :status"
            (is (spies/called? required-spy))
            (is (= ::required (get-in validator-map [:response :status]))))

          (testing "when validating :response :headers"
            (is (spies/called-with? tuple-spy ::header-key ::header-value))
            (is (spies/called-with? coll-spy ::tuple))
            (is (= ::coll (get-in validator-map [:response :headers])))))))))

(defn run-tests [] (t/run-tests))
