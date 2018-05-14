(ns com.ben-allred.clj-app-simulator.ui.views.main-test
  (:require [cljs.test :as t :refer-macros [deftest testing is]]
            [com.ben-allred.clj-app-simulator.ui.views.main :as main]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
            [test.utils.dom :as test.dom]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.ui.views.simulators :as sims]
            [com.ben-allred.clj-app-simulator.ui.views.components.core :as components]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.views :as http.views]
            [com.ben-allred.clj-app-simulator.ui.simulators.ws.views :as ws.views]
            [com.ben-allred.clj-app-simulator.utils.uuids :as uuids]))

(deftest ^:unit header-test
  (testing "(header)"
    (let [path-for-spy (spies/create (constantly ::href))]
      (with-redefs [nav/path-for path-for-spy]
        (let [header (main/header)]
          (testing "has link to home page"
            (let [link (test.dom/query-one header :a.home-link)
                  logo (test.dom/query-one link :img.logo)]
              (is (= ::href (:href (test.dom/attrs link))))
              (is (= "/images/logo.png" (:src (test.dom/attrs logo)))))

            (testing "has page title"
              (let [h1 (test.dom/query-one header :h1)]
                (is (test.dom/contains? h1 "App Simulator"))))))))))

(deftest ^:unit root-test
  (testing "(root)"
    (let [dispatch-spy (spies/create)]
      (with-redefs [store/dispatch dispatch-spy]
        (let [root (main/root {:simulators {:status ::status :data ::data}})]
          (testing "has a header"
            (is (= "Simulators" (second (test.dom/query-one root :h2)))))
          (testing "displays simulators"
            (is (= [components/with-status ::status sims/simulators ::data]
                   (butlast (test.dom/query-one root components/with-status))))
            ((last (test.dom/query-one root components/with-status)))
            (is (spies/called-with? dispatch-spy actions/request-simulators))))))))

(deftest ^:unit details-test
  (testing "(details)"
    (let [dispatch-spy (spies/create)]
      (with-redefs [store/dispatch dispatch-spy]
        (let [id #uuid "ed206e4f-2eaf-4c3e-aa1e-720be417be51"
              state {:simulators {:status ::status
                                  :data   {id {:config {:method :http/post :path "/path"}
                                               ::and   ::things}}}
                     :page       {:route-params {:id (str id)}}}]
          (testing "has a header"
            (is (test.dom/contains? (main/details state) "Simulator Details")))

          (testing "when given an http simulator"
            (spies/reset! dispatch-spy)
            (let [root (main/details state)
                  [_ & args] (test.dom/query-one root components/with-status)
                  f (last args)]
              (testing "renders the component"
                (is (= [::status http.views/sim {:config {:method :http/post :path "/path"} ::and ::things}]
                       (butlast args)))
                (f)
                (is (spies/called-with? dispatch-spy actions/request-simulators)))))

          (testing "when given a ws simulator"
            (spies/reset! dispatch-spy)
            (let [root (main/details (assoc-in state [:simulators :data id :config :method] :ws))
                  [_ & args] (test.dom/query-one root components/with-status)
                  f (last args)]
              (testing "renders the component"
                (is (= [::status ws.views/sim {:config {:method :ws :path "/path"} ::and ::things}]
                       (butlast args))))
              (f)
              (is (spies/called-with? dispatch-spy actions/request-simulators))))

          (testing "when not given a simulator"
            (let [root (main/details (update-in state [:simulators :data] dissoc id))]
              (testing "renders a spinner"
                (is (not (test.dom/query-one root components/with-status)))
                (is (test.dom/query-one root components/spinner))))))))))

(defn run-tests [] (t/run-tests))
