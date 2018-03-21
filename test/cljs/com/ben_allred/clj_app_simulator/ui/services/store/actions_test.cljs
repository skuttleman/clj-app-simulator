(ns com.ben-allred.clj-app-simulator.ui.services.store.actions-test
    (:require [cljs.test :refer-macros [deftest testing is async]]
              [cljs.core.async :as async]
              [com.ben-allred.clj-app-simulator.services.http :as http]
              [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
              [com.ben-allred.clj-app-simulator.ui.utils.macros :as macros]
              [test.utils.spies :as spies]))

(deftest request-simulators-test
    (testing "(request-simulators)"
        (let [dispatch (spies/create-spy)]
            (testing "calls dispatch with request action"
                (spies/reset-spy! dispatch)
                (actions/request-simulators [dispatch])
                (is (spies/called-with? dispatch [:simulators/request]))))))

(deftest request-simulators-success-test
    (testing "(request-simulators)"
        (testing "calls dispatch when request succeeds"
            (async done
                (async/go
                    (with-redefs [http/get (spies/spy-on
                                               (fn [_]
                                                   (async/go
                                                       [:success {:some :result}])))]
                        (let [dispatch (spies/create-spy)
                              result   (async/<! (actions/request-simulators [dispatch]))]
                            (is (spies/called-with? dispatch [:simulators/succeed {:some :result}]))
                            (is (spies/called-with? http/get "/api/simulators"))
                            (done))))))))

(deftest request-simulators-failure-test
    (testing "(request-simulators)"
        (testing "calls dispatch when request fails"
            (async done
                (async/go
                    (with-redefs [http/get
                                  (spies/spy-on (fn [_]
                                                    (async/go
                                                        [:error {:some :reason}])))]
                        (let [dispatch (spies/create-spy)
                              result   (async/<! (actions/request-simulators [dispatch]))]
                            (is (spies/called-with? dispatch [:simulators/fail {:some :reason}]))
                            (is (spies/called-with? http/get "/api/simulators"))
                            (done))))))))

(deftest show-modal-test
    (testing "(show-modal)"
        (let [dispatch    (spies/spy-on identity)
              action      (actions/show-modal ::content "Some Title")
              timeout-spy (spies/create-spy)]
            (testing "returns an action function"
                (is (fn? action)))
            (testing "mounts modal"
                (spies/reset-spy! dispatch)
                (action [dispatch])
                (is (spies/called-with? dispatch [:modal/mount ::content "Some Title"]))
                (is (spies/called-times? dispatch 1)))
            (testing "shows modal after 1 ms"
                (spies/reset-spy! dispatch)
                (spies/reset-spy! timeout-spy)
                (with-redefs [macros/set-timeout timeout-spy]
                    (action [dispatch])
                    (let [[f ms] (first (spies/get-calls timeout-spy))]
                        (f)
                        (is (spies/called-with? dispatch [:modal/show]))
                        (is (= 1 ms))))))))

(deftest hide-modal-test
    (testing "(hide-modal)"
        (let [dispatch    (spies/create-spy)
              timeout-spy (spies/create-spy)]
            (testing "hides modal"
                (spies/reset-spy! dispatch)
                (actions/hide-modal [dispatch])
                (is (spies/called-with? dispatch [:modal/hide]))
                (is (spies/called-times? dispatch 1)))
            (testing "unmounts modal"
                (spies/reset-spy! dispatch)
                (spies/reset-spy! timeout-spy)
                (with-redefs [macros/set-timeout timeout-spy]
                    (actions/hide-modal [dispatch])
                    (let [[f ms] (first (spies/get-calls timeout-spy))]
                        (f)
                        (is (spies/called-with? dispatch [:modal/unmount]))
                        (is (= 600 ms))))))))

(deftest show-toast-test
    (testing "(show-toast)"
        (let [dispatch    (spies/create-spy)
              timeout-spy (spies/create-spy)
              action      (actions/show-toast ::level "Some text")
              key         (gensym)
              gensym-spy  (spies/spy-on (constantly key))]
            (with-redefs [gensym gensym-spy]
                (testing "displays modal"
                    (spies/reset-spy! dispatch)
                    (action [dispatch])
                    (is (spies/called? gensym-spy))
                    (is (spies/called-with? dispatch
                                            [:toast/display key ::level "Some text"]))
                    (is (spies/called-times? dispatch 1)))
                (testing "unmounts modal"
                    (spies/reset-spy! dispatch)
                    (spies/reset-spy! timeout-spy)
                    (with-redefs [macros/set-timeout timeout-spy]
                        (action [dispatch])
                        (let [[f ms] (first (spies/get-calls timeout-spy))]
                            (f)
                            (is (spies/called-with? dispatch [:toast/remove key]))
                            (is (= 6000 ms)))))))))
