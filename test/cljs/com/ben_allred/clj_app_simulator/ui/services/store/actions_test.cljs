(ns com.ben-allred.clj-app-simulator.ui.services.store.actions-test
  (:require [cljs.test :as t :refer-macros [deftest testing is async]]
            [cljs.core.async :as async]
            [com.ben-allred.clj-app-simulator.services.http :as http]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.utils.macros :as macros]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.services.files :as files]))

(deftest ^:unit request-simulators-test
  (testing "(request-simulators)"
    (let [dispatch (spies/create)]
      (with-redefs [http/get (spies/constantly (async/chan))]
        (testing "calls dispatch with request action"
          (actions/request-simulators [dispatch])
          (is (spies/called-with? dispatch [:simulators.fetch-all/request])))))))

(deftest ^:unit request-simulators-success-test
  (testing "(request-simulators)"
    (testing "calls dispatch when request succeeds"
      (async done
        (async/go
          (with-redefs [http/get (spies/create
                                   (fn [_]
                                     (async/go
                                       [:success {:some :result}])))]
            (let [dispatch (spies/create)]
              (async/<! (actions/request-simulators [dispatch]))
              (is (spies/called-with? dispatch [:simulators.fetch-all/succeed {:some :result}]))
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
            (let [dispatch (spies/create)]
              (async/<! (actions/request-simulators [dispatch]))
              (is (spies/called-with? dispatch [:simulators.fetch-all/fail {:some :reason}]))
              (is (spies/called-with? http/get "/api/simulators"))
              (done))))))))

(deftest ^:unit delete-simulator-test
  (testing "(delete-simulator)"
    (let [dispatch (spies/create)]
      (with-redefs [http/delete (spies/constantly (async/chan))]
        (testing "calls dispatch with request action"
          ((actions/delete-simulator ::id) [dispatch])
          (is (spies/called-with? dispatch [:simulators.delete/request])))))))

(deftest ^:unit delete-simulator-success-test
  (testing "(delete-simulator)"
    (testing "calls dispatch when request succeeds"
      (async done
        (async/go
          (with-redefs [http/delete (spies/create
                                      (fn [_]
                                        (async/go
                                          [:success {:some :result}])))]
            (let [dispatch (spies/create)
                  f (actions/delete-simulator 123)]
              (async/<! (f [dispatch]))
              (is (spies/called-with? dispatch [:simulators.delete/succeed {:some :result}]))
              (is (spies/called-with? http/delete "/api/simulators/123"))
              (done))))))))

(deftest ^:unit delete-simulator-failure-test
  (testing "(delete-simulator)"
    (testing "calls dispatch when request fails"
      (async done
        (async/go
          (with-redefs [http/delete
                        (spies/create (fn [_]
                                        (async/go
                                          [:error {:some :reason}])))]
            (let [dispatch (spies/create)
                  f (actions/delete-simulator 999)]
              (async/<! (f [dispatch]))
              (is (spies/called-with? dispatch [:simulators.delete/fail {:some :reason}]))
              (is (spies/called-with? http/delete "/api/simulators/999"))
              (done))))))))

(deftest ^:unit create-simulator-test
  (testing "(create-simulator)"
    (let [dispatch (spies/create)]
      (with-redefs [http/post (spies/constantly (async/chan))]
        (testing "calls dispatch with request action"
          ((actions/create-simulator ::simulator) [dispatch])
          (is (spies/called-with? dispatch [:simulators.create/request])))))))

(deftest ^:unit create-simulator-success-test
  (testing "(create-simulator)"
    (testing "calls dispatch when request succeeds"
      (async done
        (async/go
          (with-redefs [http/post (spies/create
                                    (fn [_]
                                      (async/go
                                        [:success {:some :result}])))]
            (let [dispatch (spies/create)
                  f (actions/create-simulator ::simulator)]
              (async/<! (f [dispatch]))
              (is (spies/called-with? dispatch [:simulators.create/succeed {:some :result}]))
              (is (spies/called-with? http/post "/api/simulators" {:body {:simulator ::simulator}}))
              (done))))))))

(deftest ^:unit create-simulator-failure-test
  (testing "(create-simulator)"
    (testing "calls dispatch when request fails"
      (async done
        (async/go
          (with-redefs [http/post
                        (spies/create (fn [_]
                                        (async/go
                                          [:error {:some :reason}])))]
            (let [dispatch (spies/create)
                  f (actions/create-simulator ::simulator)]
              (async/<! (f [dispatch]))
              (is (spies/called-with? dispatch [:simulators.create/fail {:some :reason}]))
              (is (spies/called-with? http/post "/api/simulators" {:body {:simulator ::simulator}}))
              (done))))))))

(deftest ^:unit clear-requests-test
  (testing "(clear-requests)"
    (let [dispatch (spies/create)]
      (with-redefs [http/patch (spies/constantly (async/chan))]
        (testing "calls dispatch with request action"
          ((actions/clear-requests ::action ::simulator) [dispatch])
          (is (spies/called-with? dispatch [:simulators.clear-requests/request])))))))

(deftest ^:unit clear-requests-success-test
  (testing "(clear-requests)"
    (testing "calls dispatch when request succeeds"
      (async done
        (async/go
          (with-redefs [http/patch (spies/create
                                     (fn [_]
                                       (async/go
                                         [:success {:some :result}])))]
            (let [dispatch (spies/create)
                  f (actions/clear-requests ::action 123)]
              (async/<! (f [dispatch]))
              (is (spies/called-with? dispatch [:simulators.clear-requests/succeed {:some :result}]))
              (is (spies/called-with? http/patch "/api/simulators/123" {:body {:action ::action}}))
              (done))))))))

(deftest ^:unit clear-requests-failure-test
  (testing "(clear-requests)"
    (testing "calls dispatch when request fails"
      (async done
        (async/go
          (with-redefs [http/patch (spies/create
                                     (fn [_]
                                       (async/go
                                         [:error {:some :error}])))]
            (let [dispatch (spies/create)
                  f (actions/clear-requests ::action 123)]
              (async/<! (f [dispatch]))
              (is (spies/called-with? dispatch [:simulators.clear-requests/fail {:some :error}]))
              (done))))))))

(deftest ^:unit reset-simulator-test
  (testing "(reset-simulator)"
    (let [dispatch (spies/create)]
      (with-redefs [http/patch (spies/constantly (async/chan))]
        (testing "calls dispatch with request action"
          ((actions/reset-simulator ::id) [dispatch])
          (is (spies/called-with? dispatch [:simulators.reset/request])))))))

(deftest ^:unit reset-simulator-success-test
  (testing "(reset-simulator)"
    (testing "calls dispatch when request succeeds"
      (async done
        (async/go
          (with-redefs [http/patch (spies/create
                                     (fn [_]
                                       (async/go
                                         [:success {:some :result}])))]
            (let [dispatch (spies/create)
                  f (actions/reset-simulator 123)]
              (async/<! (f [dispatch]))
              (is (spies/called-with? dispatch [:simulators.reset/succeed {:some :result}]))
              (is (spies/called-with? http/patch "/api/simulators/123" {:body {:action :simulators/reset}}))
              (done))))))))

(deftest ^:unit reset-simulator-failure-test
  (testing "(reset-simulator)"
    (testing "calls dispatch when request fails"
      (async done
        (async/go
          (with-redefs [http/patch (spies/create
                                     (fn [_]
                                       (async/go
                                         [:error {:some :error}])))]
            (let [dispatch (spies/create)
                  f (actions/reset-simulator 123)]
              (async/<! (f [dispatch]))
              (is (spies/called-with? dispatch [:simulators.reset/fail {:some :error}]))
              (done))))))))

(deftest ^:unit update-simulator-test
  (testing "(update-simulator)"
    (let [dispatch (spies/create)]
      (with-redefs [http/patch (spies/constantly (async/chan))]
        (testing "calls dispatch with request action"
          ((actions/update-simulator ::id ::simulator) [dispatch])
          (is (spies/called-with? dispatch [:simulators.change/request])))))))

(deftest ^:unit update-simulator-success-test
  (testing "(update-simulator)"
    (testing "calls dispatch when request succeeds"
      (async done
        (async/go
          (with-redefs [http/patch (spies/create
                                     (fn [_]
                                       (async/go
                                         [:success {:some :result}])))]
            (let [dispatch (spies/create)
                  f (actions/update-simulator 123 {::some ::config})]
              (async/<! (f [dispatch]))
              (is (spies/called-with? dispatch [:simulators.change/succeed {:some :result}]))
              (is (spies/called-with? http/patch "/api/simulators/123" {:body {:action :simulators/change
                                                                               :config {::some ::config}}}))
              (done))))))))

(deftest ^:unit update-simulator-failure-test
  (testing "(update-simulator)"
    (testing "calls dispatch when request fails"
      (async done
        (async/go
          (with-redefs [http/patch (spies/create
                                     (fn [_]
                                       (async/go
                                         [:error {:some :error}])))]
            (let [dispatch (spies/create)
                  f (actions/update-simulator 123 {::some ::config})]
              (async/<! (f [dispatch]))
              (is (spies/called-with? dispatch [:simulators.change/fail {:some :error}]))
              (done))))))))

(deftest ^:unit disconnect-test
  (testing "(disconnect)"
    (let [dispatch (spies/create)]
      (with-redefs [http/patch (spies/constantly (async/go [:success {}]))]
        (testing "calls dispatch with request action"
          ((actions/disconnect ::simulator-id ::socket-id) [dispatch])
          (is (spies/called-with? dispatch [:simulators.disconnect/request])))))))

(deftest ^:unit disconnect-success-test
  (testing "(disconnect)"
    (testing "calls dispatch when request succeeds"
      (async done
        (async/go
          (with-redefs [http/patch (spies/create
                                     (fn [_]
                                       (async/go
                                         [:success {:some :result}])))]
            (let [dispatch (spies/create)
                  f (actions/disconnect 123 456)]
              (async/<! (f [dispatch]))
              (is (spies/called-with? dispatch [:simulators.disconnect/succeed {:some :result}]))
              (is (spies/called-with? http/patch "/api/simulators/123" {:body {:action    :simulators.ws/disconnect
                                                                               :socket-id 456}}))
              (done))))))))

(deftest ^:unit disconnect-failure-test
  (testing "(disconnect)"
    (testing "calls dispatch when request fails"
      (async done
        (async/go
          (with-redefs [http/patch (spies/create
                                     (fn [_]
                                       (async/go
                                         [:error {:some :error}])))]
            (let [dispatch (spies/create)
                  f (actions/disconnect 123 456)]
              (async/<! (f [dispatch]))
              (is (spies/called-with? dispatch [:simulators.disconnect/fail {:some :error}]))
              (done))))))))

(deftest ^:unit disconnect-all-test
  (testing "(disconnect-all)"
    (let [dispatch (spies/create)]
      (with-redefs [http/patch (spies/constantly (async/go [:success {}]))]
        (testing "calls dispatch with request action"
          ((actions/disconnect-all ::id) [dispatch])
          (is (spies/called-with? dispatch [:simulators.disconnect-all/request])))))))

(deftest ^:unit disconnect-all-success-test
  (testing "(disconnect-all)"
    (testing "calls dispatch when request succeeds"
      (async done
        (async/go
          (with-redefs [http/patch (spies/create
                                     (fn [_]
                                       (async/go
                                         [:success {:some :result}])))]
            (let [dispatch (spies/create)
                  f (actions/disconnect-all 123)]
              (async/<! (f [dispatch]))
              (is (spies/called-with? dispatch [:simulators.disconnect-all/succeed {:some :result}]))
              (is (spies/called-with? http/patch "/api/simulators/123" {:body {:action :simulators.ws/disconnect-all}}))
              (done))))))))

(deftest ^:unit disconnect-all-failure-test
  (testing "(disconnect-all)"
    (testing "calls dispatch when request fails"
      (async done
        (async/go
          (with-redefs [http/patch (spies/create
                                     (fn [_]
                                       (async/go
                                         [:error {:some :error}])))]
            (let [dispatch (spies/create)
                  f (actions/disconnect-all 123)]
              (async/<! (f [dispatch]))
              (is (spies/called-with? dispatch [:simulators.disconnect-all/fail {:some :error}]))
              (done))))))))

(deftest ^:unit send-message-test
  (testing "(send-message)"
    (let [dispatch (spies/create)]
      (with-redefs [http/post (spies/constantly (async/go [:success {}]))]
        (testing "calls dispatch with request action"
          ((actions/send-message ::simulator-id ::socket-id "a message") [dispatch])
          (is (spies/called-with? dispatch [:simulators.send-message/request])))))))

(deftest ^:unit send-message-success-test
  (testing "(send-message)"
    (testing "calls dispatch when request succeeds"
      (async done
        (async/go
          (with-redefs [http/post (spies/create
                                    (fn [_]
                                      (async/go
                                        [:success {:some :result}])))]
            (testing "when there is a socket id"
              (let [dispatch (spies/create)
                    f (actions/send-message 123 456 "a message")]
                (async/<! (f [dispatch]))
                (is (spies/called-with? dispatch [:simulators.send-message/succeed {:some :result}]))
                (is (spies/called-with? http/post "/api/simulators/123/456" {:body    "a message"
                                                                             :headers {:content-type "text/plain"}}))))

            (testing "when there is no socket id"
              (let [dispatch (spies/create)
                    f (actions/send-message 123 nil "a message")]
                (async/<! (f [dispatch]))
                (is (spies/called-with? dispatch [:simulators.send-message/succeed {:some :result}]))
                (is (spies/called-with? http/post "/api/simulators/123" {:body    "a message"
                                                                         :headers {:content-type "text/plain"}}))
                (done)))))))))

(deftest ^:unit send-message-failure-test
  (testing "(send-message)"
    (testing "calls dispatch when request fails"
      (async done
        (async/go
          (with-redefs [http/post
                        (spies/create (fn [_]
                                        (async/go
                                          [:error {:some :reason}])))]
            (let [dispatch (spies/create)
                  f (actions/send-message 123 456 "a message")]
              (async/<! (f [dispatch]))
              (is (spies/called-with? dispatch [:simulators.send-message/fail {:some :reason}]))
              (is (spies/called-with? http/post "/api/simulators/123/456" {:body    "a message"
                                                                           :headers {:content-type "text/plain"}}))
              (done))))))))

(deftest ^:unit upload-test
  (testing "(upload)"
    (let [dispatch (spies/create)]
      (with-redefs [files/upload (spies/constantly (async/go [:success {}]))]
        (testing "calls dispatch with request action"
          ((actions/upload ::files) [dispatch])
          (is (spies/called-with? dispatch [:files.upload/request])))))))

(deftest ^:unit upload-success-test
  (testing "(upload)"
    (testing "calls dispatch when request succeeds"
      (async done
        (async/go
          (with-redefs [files/upload (spies/create
                                       (fn [_]
                                         (async/go
                                           [:success {:some :result}])))]
            (let [dispatch (spies/create)
                  f (actions/upload ::files)]
              (async/<! (f [dispatch]))
              (is (spies/called-with? dispatch [:files.upload/succeed {:some :result}]))
              (is (spies/called-with? files/upload "/api/resources" :post ::files))
              (done))))))))

(deftest ^:unit upload-failure-test
  (testing "(upload)"
    (testing "calls dispatch when request fails"
      (async done
        (async/go
          (with-redefs [files/upload (spies/create
                                       (fn [_]
                                         (async/go
                                           [:error {:some :reason}])))]
            (let [dispatch (spies/create)
                  f (actions/upload ::files)]
              (async/<! (f [dispatch]))
              (is (spies/called-with? dispatch [:files.upload/fail {:some :reason}]))
              (is (spies/called-with? files/upload "/api/resources" :post ::files))
              (done))))))))

(deftest ^:unit upload-replace-test
  (testing "(upload-replace)"
    (let [dispatch (spies/create)]
      (with-redefs [files/upload (spies/constantly (async/go [:success {}]))]
        (testing "calls dispatch with request action"
          ((actions/upload-replace ::id ::files) [dispatch])
          (is (spies/called-with? dispatch [:files.replace/request])))))))

(deftest ^:unit upload-replace-success-test
  (testing "(upload-replace)"
    (testing "calls dispatch when request succeeds"
      (async done
        (async/go
          (with-redefs [files/upload (spies/create
                                       (fn [_]
                                         (async/go
                                           [:success {:some :result}])))]
            (let [dispatch (spies/create)
                  f (actions/upload-replace ::id ::files)]
              (async/<! (f [dispatch]))
              (is (spies/called-with? dispatch [:files.replace/succeed {:some :result}]))
              (is (spies/called-with? files/upload (str "/api/resources/" ::id) :put ::files))
              (done))))))))

(deftest ^:unit upload-replace-failure-test
  (testing "(upload-replace)"
    (testing "calls dispatch when request fails"
      (async done
        (async/go
          (with-redefs [files/upload (spies/create
                                       (fn [_]
                                         (async/go
                                           [:error {:some :reason}])))]
            (let [dispatch (spies/create)
                  f (actions/upload-replace ::id ::files)]
              (async/<! (f [dispatch]))
              (is (spies/called-with? dispatch [:files.replace/fail {:some :reason}]))
              (is (spies/called-with? files/upload (str "/api/resources/" ::id) :put ::files))
              (done))))))))

(deftest ^:unit get-uploads-test
  (testing "(get-uploads)"
    (let [dispatch (spies/create)]
      (with-redefs [http/get (constantly (async/chan))]
        (testing "calls dispatch with request action"
          (actions/get-uploads [dispatch])
          (is (spies/called-with? dispatch [:files.fetch-all/request])))))))

(deftest ^:unit get-uploads-success-test
  (testing "(get-uploads)"
    (testing "calls dispatch when request succeeds"
      (async done
        (async/go
          (with-redefs [http/get (spies/create
                                   (fn [_]
                                     (async/go
                                       [:success {:some :result}])))]
            (let [dispatch (spies/create)]
              (async/<! (actions/get-uploads [dispatch]))
              (is (spies/called-with? dispatch [:files.fetch-all/succeed {:some :result}]))
              (is (spies/called-with? http/get "/api/resources"))
              (done))))))))

(deftest ^:unit get-uploads-failure-test
  (testing "(get-uploads)"
    (testing "calls dispatch when request fails"
      (async done
        (async/go
          (with-redefs [http/get (spies/create
                                   (fn [_]
                                     (async/go
                                       [:error {:some :reason}])))]
            (let [dispatch (spies/create)]
              (async/<! (actions/get-uploads [dispatch]))
              (is (spies/called-with? dispatch [:files.fetch-all/fail {:some :reason}]))
              (is (spies/called-with? http/get "/api/resources"))
              (done))))))))

(deftest ^:unit delete-upload-test
  (testing "(delete-upload)"
    (let [dispatch (spies/create)]
      (with-redefs [http/delete (constantly (async/chan))]
        (testing "calls dispatch with request action"
          ((actions/delete-upload ::id) [dispatch])
          (is (spies/called-with? dispatch [:files.delete/request])))))))

(deftest ^:unit delete-upload-success-test
  (testing "(delete-upload)"
    (testing "calls dispatch when request succeeds"
      (async done
        (async/go
          (with-redefs [http/delete (spies/create
                                      (fn [_]
                                        (async/go
                                          [:success {:some :result}])))]
            (let [dispatch (spies/create)]
              (async/<! ((actions/delete-upload ::id) [dispatch]))
              (is (spies/called-with? dispatch [:files.delete/succeed {:id ::id} {:some :result}]))
              (is (spies/called-with? http/delete (str "/api/resources/" ::id)))
              (done))))))))

(deftest ^:unit delete-upload-failure-test
  (testing "(delete-upload)"
    (testing "calls dispatch when request fails"
      (async done
        (async/go
          (with-redefs [http/delete (spies/create
                                      (fn [_]
                                        (async/go
                                          [:error {:some :reason}])))]
            (let [dispatch (spies/create)]
              (async/<! ((actions/delete-upload ::id) [dispatch]))
              (is (spies/called-with? dispatch [:files.delete/fail {:some :reason}]))
              (is (spies/called-with? http/delete (str "/api/resources/" ::id)))
              (done))))))))

(deftest ^:unit delete-uploads-test
  (testing "(delete-uploads)"
    (let [dispatch (spies/create)]
      (with-redefs [http/delete (constantly (async/chan))]
        (testing "calls dispatch with request action"
          (actions/delete-uploads [dispatch])
          (is (spies/called-with? dispatch [:files.delete-all/request])))))))

(deftest ^:unit delete-uploads-success-test
  (testing "(delete-uploads)"
    (testing "calls dispatch when request succeeds"
      (async done
        (async/go
          (with-redefs [http/delete (spies/create
                                      (fn [_]
                                        (async/go
                                          [:success {:some :result}])))]
            (let [dispatch (spies/create)]
              (async/<! (actions/delete-uploads [dispatch]))
              (is (spies/called-with? dispatch [:files.delete-all/succeed {:some :result}]))
              (is (spies/called-with? http/delete "/api/resources"))
              (done))))))))

(deftest ^:unit delete-uploads-failure-test
  (testing "(delete-uploads)"
    (testing "calls dispatch when request fails"
      (async done
        (async/go
          (with-redefs [http/delete (spies/create
                                      (fn [_]
                                        (async/go
                                          [:error {:some :reason}])))]
            (let [dispatch (spies/create)]
              (async/<! (actions/delete-uploads [dispatch]))
              (is (spies/called-with? dispatch [:files.delete-all/fail {:some :reason}]))
              (is (spies/called-with? http/delete "/api/resources"))
              (done))))))))

(deftest ^:unit show-modal-test
  (testing "(show-modal)"
    (let [dispatch (spies/create identity)
          action (actions/show-modal ::content "Some Title" ::action-1 ::action-2)
          timeout-spy (spies/create)]
      (testing "returns an action function"
        (is (fn? action)))
      (testing "mounts modal"
        (spies/reset! dispatch)
        (action [dispatch])
        (is (spies/called-with? dispatch [:modal/mount ::content "Some Title" [::action-1 ::action-2]]))
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
    (let [dispatch (spies/create)
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
    (let [dispatch (spies/create)
          timeout-spy (spies/create)
          action (actions/show-toast ::level "Some text")
          key (gensym)
          gensym-spy (spies/constantly key)]
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

(defn run-tests []
  (t/run-tests))
