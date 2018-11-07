(ns com.ben-allred.app-simulator.ui.services.store.actions-test
  (:require
    [cljs.core.async :as async]
    [clojure.test :as t :refer-macros [are async deftest is testing]]
    [com.ben-allred.app-simulator.services.files :as files]
    [com.ben-allred.app-simulator.services.http :as http]
    [com.ben-allred.app-simulator.ui.services.store.actions :as actions]
    [com.ben-allred.app-simulator.ui.utils.macros :as macros]
    [test.utils.spies :as spies]))

(deftest ^:unit request*-test
  (testing "(request*)"
    (async done
      (async/go
        (let [dispatch (spies/create)]
          (testing "handles success responses"
            (are [input event call] (let [response [:success input]
                                          result (async/<! (actions/request*
                                                             (async/go response)
                                                             dispatch
                                                             event
                                                             nil))]
                                      (is (spies/called-with? dispatch call))
                                      (= response result))
              ::value ::success [::success ::value]
              ::value [::success] [::success ::value]
              ::value [::success ::details] [::success ::details ::value]))

          (testing "handles failure responses"
            (are [input event call] (let [response [:error input]
                                          result (async/<! (actions/request*
                                                             (async/go response)
                                                             dispatch
                                                             nil
                                                             event))]
                                      (is (spies/called-with? dispatch call))
                                      (= response result))
              ::value ::error [::error ::value]
              ::value [::error] [::error ::value]
              ::value [::error ::details] [::error ::details ::value]))

          (done))))))

(deftest ^:unit request-simulators-test
  (testing "(request-simulators)"
    (with-redefs [http/get (spies/constantly ::request)
                  actions/request* (spies/constantly ::result)]
      (testing "requests the simulators"
        (let [dispatch-spy (spies/create)
              result (actions/request-simulators [dispatch-spy])]
          (is (spies/called-with? dispatch-spy [:simulators.fetch-all/request]))
          (is (spies/called-with? http/get "/api/simulators"))
          (is (spies/called-with? actions/request*
                                  ::request
                                  dispatch-spy
                                  :simulators.fetch-all/succeed
                                  :simulators.fetch-all/fail))
          (is (= result ::result)))))))

(deftest ^:unit delete-simulator-test
  (testing "(delete-simulator)"
    (with-redefs [http/delete (spies/constantly ::request)
                  actions/request* (spies/constantly ::result)]
      (testing "deletes the simulator"
        (let [dispatch-spy (spies/create)
              result ((actions/delete-simulator 12345) [dispatch-spy])]
          (is (spies/called-with? dispatch-spy [:simulators.delete/request]))
          (is (spies/called-with? http/delete "/api/simulators/12345"))
          (is (spies/called-with? actions/request*
                                  ::request
                                  dispatch-spy
                                  :simulators.delete/succeed
                                  :simulators.delete/fail))
          (is (= result ::result)))))))

(deftest ^:unit create-simulator-test
  (testing "(create-simulator)"
    (with-redefs [http/post (spies/constantly ::request)
                  actions/request* (spies/constantly ::result)]
      (testing "creates the simulator"
        (let [dispatch-spy (spies/create)
              result ((actions/create-simulator ::simulator) [dispatch-spy])]
          (is (spies/called-with? dispatch-spy [:simulators.create/request]))
          (is (spies/called-with? http/post "/api/simulators" {:body {:simulator ::simulator}}))
          (is (spies/called-with? actions/request*
                                  ::request
                                  dispatch-spy
                                  :simulators.create/succeed
                                  :simulators.create/fail))
          (is (= result ::result)))))))

(deftest ^:unit clear-requests-test
  (testing "(clear-requests)"
    (with-redefs [http/patch (spies/constantly ::request)
                  actions/request* (spies/constantly ::result)]
      (testing "clears the simulator"
        (let [dispatch-spy (spies/create)
              result ((actions/clear-requests 12345 ::action) [dispatch-spy])]
          (is (spies/called-with? dispatch-spy [:simulators.clear-requests/request]))
          (is (spies/called-with? http/patch "/api/simulators/12345" {:body {:action :simulators/reset :type ::action}}))
          (is (spies/called-with? actions/request*
                                  ::request
                                  dispatch-spy
                                  :simulators.clear-requests/succeed
                                  :simulators.clear-requests/fail))
          (is (= result ::result)))))))

(deftest ^:unit reset-simulator-config-test
  (testing "(reset-simulator-config)"
    (with-redefs [http/patch (spies/constantly ::request)
                  actions/request* (spies/constantly ::result)]
      (testing "resets the simulator"
        (let [dispatch-spy (spies/create)
              result ((actions/reset-simulator-config 12345 :type) [dispatch-spy])]
          (is (spies/called-with? dispatch-spy [:simulators.reset/request]))
          (is (spies/called-with? http/patch "/api/simulators/12345" {:body {:action :simulators/reset
                                                                           :type   :type/config}}))
          (is (spies/called-with? actions/request*
                                  ::request
                                  dispatch-spy
                                  :simulators.reset/succeed
                                  :simulators.reset/fail))
          (is (= result ::result)))))))

(deftest ^:unit update-simulator-test
  (testing "(update-simulator)"
    (with-redefs [http/patch (spies/constantly ::request)
                  actions/request* (spies/constantly ::result)]
      (testing "updates the simulator"
        (let [dispatch-spy (spies/create)
              result ((actions/update-simulator 12345 ::config) [dispatch-spy])]
          (is (spies/called-with? dispatch-spy [:simulators.change/request]))
          (is (spies/called-with? http/patch "/api/simulators/12345" {:body {:action :simulators/change
                                                                             :config ::config}}))
          (is (spies/called-with? actions/request*
                                  ::request
                                  dispatch-spy
                                  :simulators.change/succeed
                                  :simulators.change/fail))
          (is (= result ::result)))))))

(deftest ^:unit disconnect-test
  (testing "(disconnect)"
    (with-redefs [http/patch (spies/constantly ::request)
                  actions/request* (spies/constantly ::result)]
      (testing "disconnects the web socket"
        (let [dispatch-spy (spies/create)
              result ((actions/disconnect 123 456) [dispatch-spy])]
          (is (spies/called-with? dispatch-spy [:simulators.disconnect/request]))
          (is (spies/called-with? http/patch "/api/simulators/123" {:body {:action    :simulators.ws/disconnect
                                                                         :socket-id 456}}))
          (is (spies/called-with? actions/request*
                                  ::request
                                  dispatch-spy
                                  :simulators.disconnect/succeed
                                  :simulators.disconnect/fail))
          (is (= result ::result)))))))

(deftest ^:unit disconnect-all-test
  (testing "(disconnect-all)"
    (with-redefs [http/patch (spies/constantly ::request)
                  actions/request* (spies/constantly ::result)]
      (testing "disconnects all web sockets for a simulator"
        (let [dispatch-spy (spies/create)
              result ((actions/disconnect-all 12345) [dispatch-spy])]
          (is (spies/called-with? dispatch-spy [:simulators.disconnect-all/request]))
          (is (spies/called-with? http/patch "/api/simulators/12345" {:body {:action :simulators.ws/disconnect}}))
          (is (spies/called-with? actions/request*
                                  ::request
                                  dispatch-spy
                                  :simulators.disconnect-all/succeed
                                  :simulators.disconnect-all/fail))
          (is (= result ::result)))))))

(deftest ^:unit send-message-test
  (testing "(send-message)"
    (with-redefs [http/post (spies/constantly ::request)
                  actions/request* (spies/constantly ::result)]
      (testing "sends a message"
        (let [dispatch-spy (spies/create)
              result ((actions/send-message 123 456 ::some-message) [dispatch-spy])]
          (is (spies/called-with? dispatch-spy [:simulators.send-message/request]))
          (is (spies/called-with? http/post "/api/simulators/123/sockets/456" {:body ::some-message :headers {:content-type "text/plain"}}))
          (is (spies/called-with? actions/request*
                                  ::request
                                  dispatch-spy
                                  :simulators.send-message/succeed
                                  :simulators.send-message/fail))
          (is (= result ::result))))

      (testing "broadcasts a message"
        (let [dispatch-spy (spies/create)
              result ((actions/send-message 123 nil ::some-message) [dispatch-spy])]
          (is (spies/called-with? dispatch-spy [:simulators.send-message/request]))
          (is (spies/called-with? http/post "/api/simulators/123" {:body ::some-message :headers {:content-type "text/plain"}}))
          (is (spies/called-with? actions/request*
                                  ::request
                                  dispatch-spy
                                  :simulators.send-message/succeed
                                  :simulators.send-message/fail))
          (is (= result ::result)))))))

(deftest ^:unit upload-test
  (testing "(upload)"
    (with-redefs [files/upload (spies/constantly ::request)
                  actions/request* (spies/constantly ::result)]
      (testing "uploads files"
        (let [dispatch-spy (spies/create)
              result ((actions/upload ::files) [dispatch-spy])]
          (is (spies/called-with? dispatch-spy [:files.upload/request]))
          (is (spies/called-with? files/upload "/api/resources" :post ::files))
          (is (spies/called-with? actions/request*
                                  ::request
                                  dispatch-spy
                                  :files.upload/succeed
                                  :files.upload/fail))
          (is (= result ::result)))))))

(deftest ^:unit upload-replace-test
  (testing "(upload-replace)"
    (with-redefs [files/upload (spies/constantly ::request)
                  actions/request* (spies/constantly ::result)]
      (testing "uploads files"
        (let [dispatch-spy (spies/create)
              result ((actions/upload-replace 123 ::files) [dispatch-spy])]
          (is (spies/called-with? dispatch-spy [:files.replace/request]))
          (is (spies/called-with? files/upload "/api/resources/123" :put ::files))
          (is (spies/called-with? actions/request*
                                  ::request
                                  dispatch-spy
                                  :files.replace/succeed
                                  :files.replace/fail))
          (is (= result ::result)))))))

(deftest ^:unit get-uploads-test
  (testing "(get-uploads)"
    (with-redefs [http/get (spies/constantly ::request)
                  actions/request* (spies/constantly ::result)]
      (testing "disconnects all web sockets for a simulator"
        (let [dispatch-spy (spies/create)
              result (actions/get-uploads [dispatch-spy])]
          (is (spies/called-with? dispatch-spy [:files.fetch-all/request]))
          (is (spies/called-with? http/get "/api/resources"))
          (is (spies/called-with? actions/request*
                                  ::request
                                  dispatch-spy
                                  :files.fetch-all/succeed
                                  :files.fetch-all/fail))
          (is (= result ::result)))))))

(deftest ^:unit delete-upload-test
  (testing "(delete-upload)"
    (with-redefs [http/delete (spies/constantly ::request)
                  actions/request* (spies/constantly ::result)]
      (testing "disconnects all web sockets for a simulator"
        (let [dispatch-spy (spies/create)
              result ((actions/delete-upload 123) [dispatch-spy])]
          (is (spies/called-with? dispatch-spy [:files.delete/request]))
          (is (spies/called-with? http/delete "/api/resources/123"))
          (is (spies/called-with? actions/request*
                                  ::request
                                  dispatch-spy
                                  [:files.delete/succeed {:id 123}]
                                  :files.delete/fail))
          (is (= result ::result)))))))

(deftest ^:unit delete-uploads-test
  (testing "(delete-uploads)"
    (with-redefs [http/delete (spies/constantly ::request)
                  actions/request* (spies/constantly ::result)]
      (testing "disconnects all web sockets for a simulator"
        (let [dispatch-spy (spies/create)
              result (actions/delete-uploads [dispatch-spy])]
          (is (spies/called-with? dispatch-spy [:files.delete-all/request]))
          (is (spies/called-with? http/delete "/api/resources"))
          (is (spies/called-with? actions/request*
                                  ::request
                                  dispatch-spy
                                  :files.delete-all/succeed
                                  :files.delete-all/fail))
          (is (= result ::result)))))))

(deftest ^:unit show-modal-test
  (testing "(show-modal)"
    (let [dispatch (spies/create identity)
          action (actions/show-modal ::content "Some Title" ::action-1 ::action-2)]
      (testing "returns an action function"
        (is (fn? action)))

      (testing "mounts modal"
        (spies/reset! dispatch)
        (action [dispatch])
        (is (spies/called-with? dispatch [:modal/mount ::content "Some Title" [::action-1 ::action-2]]))
        (is (spies/called-times? dispatch 1)))

      (testing "shows modal after 1 ms"
        (spies/reset! dispatch)
        (with-redefs [macros/set-timeout (spies/create)]
          (action [dispatch])
          (let [[f ms] (first (spies/calls macros/set-timeout))]
            (f)
            (is (spies/called-with? dispatch [:modal/show]))
            (is (= 1 ms))))))))

(deftest ^:unit hide-modal-test
  (testing "(hide-modal)"
    (let [dispatch (spies/create)]
      (testing "hides modal"
        (spies/reset! dispatch)
        (actions/hide-modal [dispatch])
        (is (spies/called-with? dispatch [:modal/hide]))
        (is (spies/called-times? dispatch 1)))

      (testing "unmounts modal"
        (spies/reset! dispatch)
        (with-redefs [macros/set-timeout (spies/create)]
          (actions/hide-modal [dispatch])
          (let [[f ms] (first (spies/calls macros/set-timeout))]
            (f)
            (is (spies/called-with? dispatch [:modal/unmount]))
            (is (= 600 ms))))))))

(deftest ^:unit remove-toast-test
  (testing "(remove-toast)"
    (let [dispatch (spies/create)]
      (testing "marks toast as removing"
        (spies/reset! dispatch)
        ((actions/remove-toast ::key) [dispatch])
        (is (spies/called-with? dispatch [:toast/removing ::key]))
        (is (spies/called-times? dispatch 1)))

      (testing "removes toast"
        (spies/reset! dispatch)
        (with-redefs [macros/set-timeout (spies/create)]
          ((actions/remove-toast ::key) [dispatch])
          (let [[f ms] (first (spies/calls macros/set-timeout))]
            (f)
            (is (spies/called-with? dispatch [:toast/remove ::key]))
            (is (= 201 ms))))))))

(deftest ^:unit show-toast-test
  (testing "(show-toast)"
    (let [dispatch (spies/create)
          key (gensym)]
      (with-redefs [gensym (spies/constantly key)
                    macros/set-timeout (spies/create)]
        ((actions/show-toast ::level "Some text") [dispatch])
        (let [[action-type key' level ref] (ffirst (spies/calls dispatch))]
          (testing "adds the toast"
            (is (spies/called-times? dispatch 1))
            (is (spies/called? gensym))
            (is (= :toast/adding action-type))
            (is (= key key'))
            (is (= ::level level)))

          (testing "does no async actions"
            (is (spies/never-called? macros/set-timeout)))

          (testing "when deref-ing the value"
            (testing "yields the text"
              (is (= "Some text" @ref)))

            (let [calls (spies/calls macros/set-timeout)]
              (testing "displays the toast"
                (spies/reset! dispatch)
                (let [[f] (first (filter (comp #{1} second) calls))]
                  (f)
                  (spies/called-with? dispatch [:toast/display key])))

              (testing "removes the toast"
                (spies/reset! dispatch)
                (let [[f] (first (filter (comp #{6000} second) calls))]
                  (f)
                  (spies/called-with? dispatch [:toast/remove key]))))))))))

(defn run-tests []
  (t/run-tests))
