(ns com.ben-allred.app-simulator.services.ui-reducers-test
  (:require
    [clojure.test :as t :refer [deftest is testing]]
    [com.ben-allred.app-simulator.services.ui-reducers :as reducers]))

(deftest ^:unit page-test
  (testing "(page)"
    (testing "has default state"
      (is (nil? (reducers/page))))

    (testing "handles :router/navigate"
      (is (= ::page (reducers/page ::some-state [:router/navigate ::page]))))

    (testing "returns unchanged state for any random action"
      (is (= ::same-state (reducers/page ::same-state [:any-random-action ::ignored]))))))

(deftest ^:unit modal-test
  (testing "(modal)"
    (testing "has default state"
      (is (= {:state :unmounted} (reducers/modal))))

    (testing "handles :modal/mount"
      (is (= {:state :mounted :content ::content :title ::title :actions ::actions}
             (reducers/modal ::any-state [:modal/mount ::content ::title ::actions]))))

    (testing "handles :modal/show"
      (is (= {:current :state :state :shown}
             (reducers/modal {:current :state} [:modal/show]))))

    (testing "handles :modal/hide"
      (is (= {:current :state :state :modal-hidden}
             (reducers/modal {:current :state :state ::some-state} [:modal/hide]))))

    (testing "handles :modal/unmount"
      (is (= {:state :unmounted}
             (reducers/modal {:current :state} [:modal/unmount]))))

    (testing "returns unchanged state for any random action"
      (is (= ::same-state (reducers/modal ::same-state [:any-random-action ::ignored]))))))

(deftest ^:unit simulators-test
  (testing "(simulators)"
    (testing "has default state"
      (is (= {:status :init :data {}} (reducers/simulators))))

    (testing "handles :simulators.fetch-one/request"
      (is (= {:status :pending :data {123 ::data}}
             (reducers/simulators {:data {123 ::data}} [:simulators.fetch-one/request]))))

    (testing "handles :simulators.fetch-all/request"
      (is (= {:status :pending :data {123 ::data}}
             (reducers/simulators {:data {123 ::data}} [:simulators.fetch-all/request]))))

    (testing "handles :simulators.fetch-one/succeed"
      (is (= {:status :available :data {123 {:id 123 :config ::config}}}
             (reducers/simulators {:data {123 ::data}}
                                  [:simulators.fetch-one/succeed {:simulator {:id 123 :config ::config}}]))))

    (testing "handles :simulators/clear"
      (is (= {:status :available :data {}}
             (reducers/simulators {:data ::data :status ::status} [:simulators/clear]))))

    (testing "handles :simulators.fetch-one/fail"
      (is (= {:status :failed :data {123 ::data}}
             (reducers/simulators {:data {123 ::data}} [:simulators.fetch-one/fail ::reason]))))

    (testing "handles :simulators.fetch-all/fail"
      (is (= {:status :failed :data {123 ::data}}
             (reducers/simulators {:data {123 ::data}} [:simulators.fetch-all/fail ::reason]))))

    (testing "handles :simulators.activity/receive"
      (is (= {:status :available :data {999 {:id 999 :requests [{:id 123} {:id 456}] :other ::stuff}}}
             (reducers/simulators {:data {999 {:id 999 :other ::stuff :requests [{:id 123}]}}}
                                  [:simulators.activity/receive {:simulator {:id 999} :request {:id 456}}]))))

    (testing "handles :simulators.activity/add"
      (is (= {:status :available :data {123 ::simulator 456 {:id 456 ::other ::stuff}}}
             (reducers/simulators {:data {123 ::simulator}}
                                  [:simulators.activity/add {:simulator {:id 456 ::other ::stuff}}]))))

    (testing "handles :simulators.activity/delete"
      (is (= {:status :available :data {123 ::simulator-1}}
             (reducers/simulators {:data {123 ::simulator-1 456 ::simulator-2}}
                                  [:simulators.activity/delete {:simulator {:id 456}}]))))

    (testing "handles :simulators.activity/reset"
      (is (= {:status :available :data {123 {:id 123 :config ::config :requests ::requests :sockets ::sockets}}}
             (reducers/simulators {:data {123 {:id 123 :config ::old-config :requests ::old-requests :sockets ::old-sockets}}}
                                  [:simulators.activity/reset
                                   {:simulator {:id 123 :config ::config :requests ::requests :sockets ::sockets}}]))))

    (testing "handles :simulators.activity/connect"
      (is (= {:status :available :data {123 {:id 123 :sockets #{999} ::some ::data}}}
             (reducers/simulators {:data {123 {:id 123 ::some ::data}} :status :available}
                                  [:simulators.activity/connect
                                   {:simulator {:id 123} :socket-id 999}]))))

    (testing "handles :simulators.activity/disconnect"
      (is (= {:status :available :data {123 {:id 123 :sockets #{} ::some ::data}}}
             (reducers/simulators {:data {123 {:id 123 ::some ::data :sockets #{999}}} :status :available}
                                  [:simulators.activity/disconnect
                                   {:simulator {:id 123} :socket-id 999}]))))

    (testing "returns unchanged state for any random action"
      (is (= {:data {123 ::data}}
             (reducers/simulators {:data {123 ::data}} [:any-random-action ::ignored]))))))

(deftest ^:unit toasts-test
  (testing "(toasts)"
    (testing "has default state"
      (is (= {} (reducers/toasts))))

    (testing "handles :toast/adding"
      (is (= {::old ::value ::key {:level ::level :ref ::ref :adding? true}}
             (reducers/toasts {::old ::value} [:toast/adding ::key ::level ::ref]))))

    (testing "handles :toast/removing"
      (is (= {::old ::value ::key {:key ::key :removing? true}}
             (reducers/toasts {::old ::value ::key {:key ::key}} [:toast/removing ::key])))
      (is (= {::old ::value}
             (reducers/toasts {::old ::value} [:toast/removing ::key]))))

    (testing "handles :toast/display"
      (is (= {::some ::toast ::key {::some ::data}}
             (reducers/toasts {::some ::toast ::key {:adding? ::adding ::some ::data}} [:toast/display ::key ::level ::text]))))

    (testing "handles :toast/unmount"
      (is (= {::some-other ::toast}
             (reducers/toasts {::some ::toast ::some-other ::toast} [:toast/remove ::some]))))

    (testing "returns unchanged state for any random action"
      (is (= ::same-state (reducers/toasts ::same-state [:any-random-action ::ignored]))))))

(deftest ^:unit resources-test
  (testing "(resources)"
    (testing "has default state"
      (is (= {:status :init :data []} (reducers/resources))))

    (testing "handles :files.fetch-all/fail"
      (is (= {:status :failed :data ::data}
             (reducers/resources {:status :available :data ::data}
                               [:files.fetch-all/fail]))))

    (testing "handles :files.fetch-all/request"
      (is (= {:status :pending :data ::data}
             (reducers/resources {:status :available :data ::data}
                               [:files.fetch-all/request]))))

    (testing "handles :files.upload/succeed"
      (is (= {:status :available :data [::file-1 ::file-2 ::file-3 ::file-4]}
             (reducers/resources {:status :pending :data [::file-1 ::file-2]}
                               [:files.upload/succeed {:resources [::file-3 ::file-4]}]))))

    (testing "handles :files.fetch-all/succeed"
      (is (= {:status :available :data [::file-3 ::file-4]}
             (reducers/resources {:status :pending :data [::file-1 ::file-2]}
                               [:files.fetch-all/succeed {:resources [::file-3 ::file-4]}]))))

    (testing "handles :files.replace/succeed"
      (is (= {:status :available :data [{:id 111 :data ::data-111} {:id 222 :data ::data-new}]}
             (reducers/resources {:data [{:id 111 :data ::data-111} {:id 222 :data ::data-222}]}
                                 [:files.replace/succeed {:resource {:id 222 :data ::data-new}}])))
      (is (= {:status :available :data [{:id 111 :data ::data-111} {:id 222 :data ::data-new}]}
             (reducers/resources {:data [{:id 111 :data ::data-111}]}
                                 [:files.replace/succeed {:resource {:id 222 :data ::data-new}}]))))

    (testing "returns unchanged state for any random action"
      (is (= {:status ::some-status :data ::same-state}
             (reducers/resources {:status ::some-status :data ::same-state} [:any-random-action ::ignored]))))))

(defn run-tests []
  (t/run-tests))
