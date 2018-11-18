(ns com.ben-allred.app-simulator.utils.chans-test
  (:require
    #?@(:clj  [[clojure.test :as t :refer [are deftest is testing]]
               [test.utils.async :refer [async]]]
        :cljs [[cljs.test :as t :refer [are async deftest is testing]]])
    [clojure.core.async :as async]
    [com.ben-allred.app-simulator.utils.chans :as ch]
    [test.utils.spies :as spies]))

(deftest ^:unit resolve-test
  (testing "(resolve)"
    (async done
      (async/go
        (testing "resolves with a value"
          (is (= [:success nil] (async/<! (ch/resolve))))
          (is (= [:success ::value] (async/<! (ch/resolve ::value)))))

        (done)))))

(deftest ^:unit reject-test
  (testing "(reject)"
    (async done
      (async/go
        (testing "rejects with a value"
          (is (= [:error nil] (async/<! (ch/reject))))
          (is (= [:error ::value] (async/<! (ch/reject ::value)))))

        (done)))))

(deftest ^:unit catch-test
  (testing "(catch)"
    (async done
      (async/go
        (testing "ignores success values"
          (let [ch (async/go [:success ::value])
                spy (spies/create)
                result (async/<! (ch/catch ch spy))]
            (is (spies/never-called? spy))
            (is (= result [:success ::value]))))

        (testing "normal responses become success"
          (let [ch (async/go [:error ::value])
                spy (spies/constantly ::ok)
                result (async/<! (ch/catch ch spy))]
            (is (spies/called-with? spy ::value))
            (is (= result [:success ::ok]))))

        (testing "returning a channel produces the new response"
          (are [ch-value] (let [ch (async/go [:error ::value])
                                result (async/<! (ch/catch ch (spies/constantly (async/go ch-value))))]
                            (= result ch-value))
            [:success ::ftw]
            [:error ::ftw]))

        (testing "returns rejection if callback throws"
          (let [ch (async/go [:error ::value])
                ex (ex-info "An exception" {:some :data})
                spy (spies/create (fn [_] (throw ex)))
                result (async/<! (ch/catch ch spy))]
            (is (= result [:error ex]))))

        (done)))))

(deftest ^:unit then-tests
  (testing "(then)"
    (async done
      (async/go
        (testing "when called with one callback"
          (testing "ignores error values"
            (let [ch (async/go [:error ::value])
                  spy (spies/create)
                  result (async/<! (ch/then ch spy))]
              (is (spies/never-called? spy))
              (is (= result [:error ::value]))))

          (testing "normal responses become success"
            (let [ch (async/go [:success ::value])
                  spy (spies/constantly ::ok)
                  result (async/<! (ch/then ch spy))]
              (is (spies/called-with? spy ::value))
              (is (= result [:success ::ok]))))

          (testing "returning a channel produces the new response"
            (are [ch-value] (let [ch (async/go [:success ::value])
                                  result (async/<! (ch/then ch (spies/constantly (async/go ch-value))))]
                              (= result ch-value))
              [:success ::ftw]
              [:error ::ftw]))

          (testing "returns rejection if callback throws"
            (let [ch (async/go [:success ::value])
                  ex (ex-info "An exception" {:some :data})
                  spy (spies/create (fn [_] (throw ex)))
                  result (async/<! (ch/then ch spy))]
              (is (= result [:error ex])))))

        (testing "when called with two callbacks"
          (testing "handles success values with on-success"
            (let [ch (async/go [:success ::value])
                  on-success (spies/constantly ::success'd)
                  on-error (spies/constantly ::error'd)
                  result (async/<! (ch/then ch on-success on-error))]
              (is (spies/called-with? on-success ::value))
              (is (spies/never-called? on-error))
              (is (= result [:success ::success'd]))))

          (testing "handles error values with on-error"
            (let [ch (async/go [:error ::value])
                  on-success (spies/constantly ::success'd)
                  on-error (spies/constantly ::error'd)
                  result (async/<! (ch/then ch on-success on-error))]
              (is (spies/never-called? on-success))
              (is (spies/called-with? on-error ::value))
              (is (= result [:success ::error'd]))))

          (testing "and when on-success produces an error"
            (testing "handles error with on-error"
              (let [ch (async/go [:success ::value])
                    on-success (spies/constantly (async/go [:error ::error]))
                    on-error (spies/constantly ::error'd)
                    result (async/<! (ch/then ch on-success on-error))]
                (is (spies/called-with? on-success ::value))
                (is (spies/called-with? on-error ::error))
                (is (= result [:success ::error'd]))))))

        (done)))))

(deftest ^:unit peek-test
  (testing "(peek)"
    (async done
      (async/go
        (testing "when called with one callback"
          (testing "peeks on the channel with a success value"
            (let [on-success (spies/create)
                  result (async/<! (ch/peek (async/go [:success ::value]) on-success))]
              (is (spies/called-with? on-success ::value))
              (is (= [:success ::value] result))))

          (testing "does not peek on the channel with an error value"
            (let [on-success (spies/create)
                  result (async/<! (ch/peek (async/go [:error ::value]) on-success))]
              (is (spies/never-called? on-success))
              (is (= [:error ::value] result))))

          (testing "swallows exceptions from peek fn"
            (let [on-success (spies/create (fn [_] (throw (ex-info "An exception" {:some :data}))))
                  result (async/<! (ch/peek (async/go [:success ::value]) on-success))]
              (is (= [:success ::value] result)))))

        (testing "when called with on-success and on-error callbacks"
          (testing "peeks on the channel with a success value"
            (let [on-success (spies/create)
                  on-error (spies/create)
                  result (async/<! (ch/peek (async/go [:success ::value]) on-success on-error))]
              (is (spies/called-with? on-success ::value))
              (is (spies/never-called? on-error))
              (is (= [:success ::value] result))))

          (testing "peeks on the channel with an error value"
            (let [on-success (spies/create)
                  on-error (spies/create)
                  result (async/<! (ch/peek (async/go [:error ::ex]) on-success on-error))]
              (is (spies/never-called? on-success))
              (is (spies/called-with? on-error ::ex))
              (is (= [:error ::ex] result))))

          (testing "swallows exceptions from on-success fn"
            (let [on-success (spies/create (fn [_] (throw (ex-info "An exception" {:some :data}))))
                  on-error (spies/create)
                  result (async/<! (ch/peek (async/go [:success ::value]) on-success on-error))]
              (is (= [:success ::value] result))))

          (testing "swallows exceptions from on-error fn"
            (let [on-success (spies/create)
                  on-error (spies/create (fn [_] (throw (ex-info "An exception" {:some :data}))))
                  result (async/<! (ch/peek (async/go [:error ::value]) on-success on-error))]
              (is (= [:error ::value] result)))))

        (done)))))

(deftest ^:unit finally-test
  (testing "(finally)"
    (async done
      (async/go
        (testing "calls the callback"
          (let [spy (spies/create)]
            (async/<! (ch/finally (async/go [:success ::value]) spy))
            (is (spies/called-with? spy))))

        (testing "when the value is a success"
          (testing "and when the callback returns a value"
            (let [result (async/<! (ch/finally (async/go [:success ::value])
                                               (constantly ::finally'd)))]
              (testing "returns the original value"
                (is (= [:success ::value] result)))))

          (testing "and when the callback returns a success channel"
            (let [result (async/<! (ch/finally (async/go [:success ::value])
                                               (constantly (async/go [:success ::finally'd]))))]
              (testing "returns the original value"
                (is (= [:success ::value] result)))))

          (testing "and when the callback returns an error channel"
            (let [result (async/<! (ch/finally (async/go [:success ::value])
                                               (constantly (async/go [:error ::finally'd]))))]
              (testing "returns the error value"
                (is (= [:error ::finally'd] result)))))

          (testing "and when the callback throws"
            (let [ex (ex-info "An exception" {:some :data})
                  result (async/<! (ch/finally (async/go [:success ::value])
                                               (fn [] (throw ex))))]
              (testing "returns the exception"
                (is (= [:error ex] result))))))

        (testing "when the value is an error"
          (testing "and when the callback returns a value"
            (let [result (async/<! (ch/finally (async/go [:error ::ex])
                                               (constantly ::finally'd)))]
              (testing "returns the original value"
                (is (= [:error ::ex] result)))))

          (testing "and when the callback returns a success channel"
            (let [result (async/<! (ch/finally (async/go [:error ::ex])
                                               (constantly (async/go [:success ::finally'd]))))]
              (testing "returns the original value"
                (is (= [:error ::ex] result)))))

          (testing "and when the callback returns an error channel"
            (let [result (async/<! (ch/finally (async/go [:error ::ex])
                                               (constantly (async/go [:error ::finally'd]))))]
              (testing "returns the error value"
                (is (= [:error ::finally'd] result)))))

          (testing "and when the callback throws"
            (let [ex (ex-info "An exception" {:some :data})
                  result (async/<! (ch/finally (async/go [:error ::ex])
                                               (fn [] (throw ex))))]
              (testing "returns the exception"
                (is (= [:error ex] result))))))

        (done)))))

(defn run-tests []
  (t/run-tests))
