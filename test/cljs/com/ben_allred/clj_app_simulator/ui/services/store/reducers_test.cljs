(ns com.ben-allred.clj-app-simulator.ui.services.store.reducers-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [com.ben-allred.clj-app-simulator.ui.services.store.reducers :as reducers]))

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
      (is (= {:state :mounted :content ::content :title ::title}
             (reducers/modal ::any-state [:modal/mount ::content ::title]))))
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
