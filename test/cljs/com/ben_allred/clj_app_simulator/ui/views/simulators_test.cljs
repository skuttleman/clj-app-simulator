(ns com.ben-allred.clj-app-simulator.ui.views.simulators-test
  (:require [cljs.test :as t :refer-macros [deftest testing is]]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
            [com.ben-allred.clj-app-simulator.ui.views.simulators :as sims]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [test.utils.dom :as test.dom]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.views :as shared.views]))

(deftest ^:unit sim-card-test
  (testing "(sim-card)"
    (let [nav-spy (spies/create)]
      (with-redefs [nav/navigate! nav-spy]
        (let [request-count (inc (rand-int 100))
              socket-count (inc (rand-int 25))
              id (rand-int 100000)
              sim {:config   {:path        "/some/path"
                              :method      :some/method
                              :name        "A Name"
                              :description "describing the things."}
                   :requests (take request-count (range))
                   :sockets (take socket-count (range))
                   :id       id}
              sim-card (sims/sim-card sim)]
          (testing "when the element is clicked"
            (-> sim-card
                (test.dom/query-one :.sim-card.button)
                (test.dom/simulate-event :click))
            (testing "navigates to details page"
              (is (spies/called-with? nav-spy :details {:id id}))))

          (testing "shows the details"
            (-> sim-card
                (test.dom/query-one shared.views/sim-details)
                (= [shared.views/sim-details sim])
                (is)))

          (testing "when name is not nil"
            (testing "shows the name"
              (-> sim-card
                  (test.dom/query-one :div.sim-card-name)
                  (test.dom/contains? "A Name")
                  (is))))

          (testing "when description is not nil"
            (testing "shows the description"
              (-> sim-card
                  (test.dom/query-one :div.sim-card-description)
                  (test.dom/contains? "describing the things.")
                  (is))))

          (testing "when there are one or more requests"
            (testing "shows the request count"
              (-> sim-card
                  (test.dom/query-one :div.sim-card-request-count)
                  (test.dom/contains? request-count)
                  (is))))

          (testing "when there are one or more sockets"
            (testing "shows the socket count"
              (-> sim-card
                  (test.dom/query-one :div.sim-card-socket-count)
                  (test.dom/contains? socket-count)
                  (is)))))
        (let [sim-card (sims/sim-card {:method   :some/method
                                       :path     "/some/path"
                                       :requests []})]

          (testing "when name is nil"
            (testing "does not show the name"
              (-> sim-card
                  (test.dom/contains? :.sim-card-name)
                  (not)
                  (is))))

          (testing "when description is nil"
            (testing "does not show the description"
              (-> sim-card
                  (test.dom/contains? :.sim-card-description)
                  (not)
                  (is))))

          (testing "when there are no requests"
            (testing "does not show the request count"
              (-> sim-card
                  (test.dom/contains? :.sim-card-request-count)
                  (not)
                  (is))))

          (testing "when there are no sockets"
            (testing "does not show the socket count"
              (-> sim-card
                  (test.dom/contains? :.sim-card-socket-count)
                  (not)
                  (is)))))))))

(deftest ^:unit sim-group-test
  (testing "(sim-group)"
    (testing "shows the group name"
      (let [sim-group (sims/sim-group "a group" nil)]
        (-> sim-group
            (test.dom/query-one :.group-name)
            (test.dom/contains? "a group")
            (is))))
    (testing "when there is no group name"
      (let [sim-group (sims/sim-group nil nil)]
        (testing "shows 'none'"
          (-> sim-group
              (test.dom/query-one :.group-name)
              (test.dom/contains? "none")
              (is)))))
    (testing "when showing a list of sim-cards"
      (let [sim-group (sims/sim-group nil [{:id 123 ::other ::data}
                                           {:id 456 ::more ::things}])
            sim-list (test.dom/query-one sim-group :.grouped-sims)
            [first-sim second-sim] (test.dom/query-all sim-list sims/sim-card)]
        (testing "includes the first simulator"
          (is (= [sims/sim-card {:id 123 ::other ::data}]
                 first-sim))
          (is (= "123" (:key (meta first-sim)))))
        (testing "includes the second simulator"
          (is (= [sims/sim-card {:id 456 ::more ::things}]
                 second-sim))
          (is (= "456" (:key (meta second-sim)))))))))

(deftest ^:unit sim-section-test
  (testing "(sim-section)"
    (let [sims [{:config {:group ::group-1 :path "path" :method :method-b ::data ::1}}
                {:config {:group ::group-2 :path "path-2" :method :method ::data ::2}}
                {:config {:group ::group-1 :path "path" :method :method-a ::data ::3}}
                {:config {:group ::group-2 :path "path-1" :method :method ::data ::4}}
                {:config {:path "path" :method :method ::data ::5}}]
          root (sims/sim-section "section" sims)]
      (testing "displays the section"
        (let [node (test.dom/query-one root :.sim-section-title)]
          (is (test.dom/contains? node "SECTION"))))

      (testing "organizes the simulator data"
        (let [[sim-group-1 sim-group-2 sim-group-3] (test.dom/query-all root sims/sim-group)]
          (is (= [sims/sim-group ::group-1 [(get sims 2) (get sims 0)]]
                 sim-group-1))
          (is (= (str ::group-1) (:key (meta sim-group-1))))
          (is (= [sims/sim-group ::group-2 [(get sims 3) (get sims 1)]]
                 sim-group-2))
          (is (= (str ::group-2) (:key (meta sim-group-2))))
          (is (= [sims/sim-group nil [(get sims 4)]]
                 sim-group-3))
          (is (= "" (:key (meta sim-group-3)))))))))

(deftest ^:unit simulators-test
  (testing "(simulators)"
    (let [sims {111 {:config {:group ::group-1 :path "path" :method :thing/a ::data ::1}}
                222 {:config {:group ::group-2 :path "path-2" :method :thing/b ::data ::2}}
                333 {:config {:group ::group-1 :path "path" :method :another ::data ::3}}
                444 {:config {:group ::group-2 :path "path-1" :method :thing/a ::data ::4}}
                555 {:config {:path "path" :method :another ::data ::5}}}
          [section-1 section-2] (-> sims (sims/simulators)
                                    (test.dom/query-all sims/sim-section))]
      (testing "groups by method"
        (is (= [sims/sim-section "another" [(get sims 333) (get sims 555)]]
               section-1))
        (is (= "another" (:key (meta section-1))))
        (is (= [sims/sim-section "thing" [(get sims 111) (get sims 222) (get sims 444)]]
               section-2))
        (is (= "thing" (:key (meta section-2))))))))

(defn run-tests [] (t/run-tests))
