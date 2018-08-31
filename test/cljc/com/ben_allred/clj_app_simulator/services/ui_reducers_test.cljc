(ns com.ben-allred.clj-app-simulator.services.ui-reducers-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.services.ui-reducers :as reducers]))

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
      (is (= {:status :pending :data ::data}
             (reducers/simulators {:data ::data} [:simulators.fetch-one/request]))))

    (testing "handles :simulators.fetch-all/request"
      (is (= {:status :pending :data ::data}
             (reducers/simulators {:data ::data} [:simulators.fetch-all/request]))))

    (testing "handles :simulators.fetch-one/succeed"
      (is (= {:status :available :data {123 {:id 123 :config ::config}}}
             (reducers/simulators {:data {123 ::data}}
                                  [:simulators.fetch-one/succeed {:simulator {:id 123 :config ::config}}]))))

    (testing "handles :simulators/clear"
      (is (= {:status :available :data {}}
             (reducers/simulators {:data ::data :status ::status} [:simulators/clear]))))

    (testing "handles :simulators.fetch-one/fail"
      (is (= {:status :failed :data ::data}
             (reducers/simulators {:data ::data} [:simulators.fetch-one/fail ::reason]))))

    (testing "handles :simulators.fetch-all/fail"
      (is (= {:status :failed :data ::data}
             (reducers/simulators {:data ::data} [:simulators.fetch-all/fail ::reason]))))

    (testing "handles :simulators.activity/receive"
      (is (= {:status :available :data {999 {:id 999 :requests [123 456] :other ::stuff}}}
             (reducers/simulators {:data {999 {:id 999 :other ::stuff :requests [123]}}}
                                  [:simulators.activity/receive {:simulator {:id 999} :request 456}]))))

    (testing "handles :simulators.activity/add"
      (is (= {:status :available :data {123 ::simulator 456 {:id 456 ::other ::stuff}}}
             (reducers/simulators {:data {123 ::simulator}}
                                  [:simulators.activity/add {:simulator {:id 456 ::other ::stuff}}]))))

    (testing "handles :simulators.activity/delete"
      (is (= {:status :available :data {123 ::simulator-1}}
             (reducers/simulators {:data {123 ::simulator-1 456 ::simulator-2}}
                                  [:simulators.activity/delete {:id 456}]))))

    (testing "handles :simulators.activity/reset-requests"
      (is (= {:status :available :data {123 {:id 123 ::some ::data :requests []}}}
             (reducers/simulators {:data {123 {:id 123 ::some ::data}}}
                                  [:simulators.activity/reset-requests
                                   {:simulator {:id 123 ::some ::data :requests ::requests}}]))))

    (testing "handles :simulators.activity/connect"
      (is (= {:status :available :data {123 {:id 123 :sockets #{999} ::some ::data}}}
             (reducers/simulators {:data {123 {:id 123 ::some ::data}} :status :available}
                                  [:simulators.activity/connect
                                   {:simulator {:id 123 :socket-id 999}}]))))

    (testing "handles :simulators.activity/disconnect"
      (is (= {:status :available :data {123 {:id 123 :sockets #{} ::some ::data}}}
             (reducers/simulators {:data {123 {:id 123 ::some ::data :sockets #{999}}} :status :available}
                                  [:simulators.activity/disconnect
                                   {:simulator {:id 123 :socket-id 999}}]))))

    (testing "returns unchanged state for any random action"
      (is (= {:data ::data}
             (reducers/simulators {:data ::data} [:any-random-action ::ignored]))))))

(deftest ^:unit toasts-test
  (testing "(toasts)"
    (testing "has default state"
      (is (= {} (reducers/toasts))))

    (testing "handles :toast/display"
      (is (= {::some ::toast ::key {:level ::level :text ::text}}
             (reducers/toasts {::some ::toast} [:toast/display ::key ::level ::text]))))

    (testing "handles :toast/unmount"
      (is (= {::some-other ::toast}
             (reducers/toasts {::some ::toast ::some-other ::toast} [:toast/remove ::some]))))

    (testing "returns unchanged state for any random action"
      (is (= ::same-state (reducers/toasts ::same-state [:any-random-action ::ignored]))))))

(deftest ^:unit uploads-test
  (testing "(uploads)"
    (testing "has default state"
      (is (= {:status :init :data []} (reducers/uploads))))

    (testing "handles :files.fetch-all/fail"
      (is (= {:status :failed :data ::data}
             (reducers/uploads {:status :available :data ::data}
                               [:files.fetch-all/fail]))))

    (testing "handles :files.fetch-all/request"
      (is (= {:status :pending :data ::data}
             (reducers/uploads {:status :available :data ::data}
                               [:files.fetch-all/request]))))

    (testing "handles :files.upload/succeed"
      (is (= {:status :available :data [::file-1 ::file-2 ::file-3 ::file-4]}
             (reducers/uploads {:status :pending :data [::file-1 ::file-2]}
                               [:files.upload/succeed [::file-3 ::file-4]]))))

    (testing "handles :files.fetch-all/succeed"
      (is (= {:status :available :data [::file-3 ::file-4]}
             (reducers/uploads {:status :pending :data [::file-1 ::file-2]}
                               [:files.fetch-all/succeed {:uploads [::file-3 ::file-4]}]))))

    (testing "returns unchanged state for any random action"
      (is (= {:status ::some-status :data ::same-state}
             (reducers/uploads {:status ::some-status :data ::same-state} [:any-random-action ::ignored]))))))
