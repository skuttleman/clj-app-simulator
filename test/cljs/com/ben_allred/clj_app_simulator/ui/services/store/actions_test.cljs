(ns com.ben-allred.clj-app-simulator.ui.services.store.actions-test
    (:require [cljs.test :refer-macros [deftest testing is async]]
              [cljs.core.async :as async]
              [com.ben-allred.clj-app-simulator.services.http :as http]
              [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
              [com.ben-allred.clj-app-simulator.ui.utils.macros :as macros]
              [test.utils.spies :as spies]))

(deftest ^:unit request-simulators-test
    (testing "(request-simulators)"
        (let [dispatch (spies/create)]
            (testing "calls dispatch with request action"
                (spies/reset! dispatch)
                (actions/request-simulators [dispatch])
                (is (spies/called-with? dispatch [:simulators/request]))))))

(deftest ^:unit request-simulators-success-test
    (testing "(request-simulators)"
        (testing "calls dispatch when request succeeds"
            (async done
                (async/go
                    (with-redefs [http/get (spies/create
                                               (fn [_]
                                                   (async/go
                                                       [:success {:some :result}])))]
                        (let [dispatch (spies/create)
                              result   (async/<! (actions/request-simulators [dispatch]))]
                            (is (spies/called-with? dispatch [:simulators/succeed {:some :result}]))
                            (is (spies/called-with? http/get "/api/simulators"))
                            (done))))))))

(deftest ^:unit request-simulators-failure-test
    (testing "(request-simulators)"
        (testing "calls dispatch when request fails"
            (async done
                (async/go
                    (with-redefs [http/get
                                  (spies/create (fn [_]
                                                    (async/go
                                                        [:error {:some :reason}])))]
                        (let [dispatch (spies/create)
                              result   (async/<! (actions/request-simulators [dispatch]))]
                            (is (spies/called-with? dispatch [:simulators/fail {:some :reason}]))
                            (is (spies/called-with? http/get "/api/simulators"))
                            (done))))))))

(deftest ^:unit show-modal-test
    (testing "(show-modal)"
        (let [dispatch    (spies/create identity)
              action      (actions/show-modal ::content "Some Title")
              timeout-spy (spies/create)]
            (testing "returns an action function"
                (is (fn? action)))
            (testing "mounts modal"
                (spies/reset! dispatch)
                (action [dispatch])
                (is (spies/called-with? dispatch [:modal/mount ::content "Some Title"]))
                (is (spies/called-times? dispatch 1)))
            (testing "shows modal after 1 ms"
                (spies/reset! dispatch)
                (spies/reset! timeout-spy)
                (with-redefs [macros/set-timeout timeout-spy]
                    (action [dispatch])
                    (let [[f ms] (first (spies/calls timeout-spy))]
                        (f)
                        (is (spies/called-with? dispatch [:modal/show]))
                        (is (= 1 ms))))))))

(deftest ^:unit hide-modal-test
    (testing "(hide-modal)"
        (let [dispatch    (spies/create)
              timeout-spy (spies/create)]
            (testing "hides modal"
                (spies/reset! dispatch)
                (actions/hide-modal [dispatch])
                (is (spies/called-with? dispatch [:modal/hide]))
                (is (spies/called-times? dispatch 1)))
            (testing "unmounts modal"
                (spies/reset! dispatch)
                (spies/reset! timeout-spy)
                (with-redefs [macros/set-timeout timeout-spy]
                    (actions/hide-modal [dispatch])
                    (let [[f ms] (first (spies/calls timeout-spy))]
                        (f)
                        (is (spies/called-with? dispatch [:modal/unmount]))
                        (is (= 600 ms))))))))

(deftest ^:unit show-toast-test
    (testing "(show-toast)"
        (let [dispatch    (spies/create)
              timeout-spy (spies/create)
              action      (actions/show-toast ::level "Some text")
              key         (gensym)
              gensym-spy  (spies/create (constantly key))]
            (with-redefs [gensym gensym-spy]
                (testing "displays modal"
                    (spies/reset! dispatch)
                    (action [dispatch])
                    (is (spies/called? gensym-spy))
                    (is (spies/called-with? dispatch
                                            [:toast/display key ::level "Some text"]))
                    (is (spies/called-times? dispatch 1)))
                (testing "unmounts modal"
                    (spies/reset! dispatch)
                    (spies/reset! timeout-spy)
                    (with-redefs [macros/set-timeout timeout-spy]
                        (action [dispatch])
                        (let [[f ms] (first (spies/calls timeout-spy))]
                            (f)
                            (is (spies/called-with? dispatch [:toast/remove key]))
                            (is (= 6000 ms)))))))))
