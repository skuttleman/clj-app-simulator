(ns com.ben-allred.clj-app-simulator.ui.views.main-test
  (:require [cljs.test :as t :refer-macros [deftest testing is]]
            [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.simulators.file.views :as file.views]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.views :as http.views]
            [com.ben-allred.clj-app-simulator.ui.simulators.ws.views :as ws.views]
            [com.ben-allred.clj-app-simulator.ui.views.components.core :as components]
            [com.ben-allred.clj-app-simulator.ui.views.main :as main]
            [com.ben-allred.clj-app-simulator.ui.views.simulators :as sims]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [test.utils.dom :as test.dom]
            [test.utils.spies :as spies]))

(deftest ^:unit header-test
  (testing "(header)"
    (let [path-for-spy (spies/constantly ::href)]
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
  (let [path-for-spy (spies/constantly ::nav)
        dispatch-spy (spies/create)
        action-spy (spies/constantly ::action)]
    (with-redefs [nav/path-for path-for-spy
                  store/dispatch dispatch-spy
                  actions/upload action-spy]
      (testing "(root)"
        (let [root (main/root {:simulators {:status ::status :data ::data}})]
          (testing "has a header"
            (is (= "Simulators" (second (test.dom/query-one root :h2)))))

          (testing "has a create menu"
            (let [[_ {:keys [items]} element] (test.dom/query-one root components/menu)]
              (is (= [{:href ::nav :label "HTTP Simulator"} {:href ::nav :label "WS Simulator"}]
                     items))
              (is (spies/called-with? path-for-spy :new {:query-params {:type :http}}))
              (is (spies/called-with? path-for-spy :new {:query-params {:type :ws}}))
              (is (-> element
                      (test.dom/query-one :.button-success)
                      (test.dom/contains? "Create")))))

          (testing "has an upload button"
            (let [{:keys [on-change]} (-> root
                                          (test.dom/query-one components/upload)
                                          (test.dom/attrs))]
              (on-change ::files)
              (is (spies/called-with? action-spy ::files))
              (is (spies/called-with? dispatch-spy ::action))))

          (testing "displays simulators"
            (is (= [components/with-status sims/simulators {:status ::status :data ::data}]
                   (test.dom/query-one root components/with-status)))))))))

(deftest ^:unit details-test
  (testing "(details)"
    (let [id #uuid "ed206e4f-2eaf-4c3e-aa1e-720be417be51"
          state {:simulators {:status ::status
                              :data   {id {:config {:method :http/post :path "/path"}
                                           ::and   ::things}}}
                 :page       {:route-params {:id (str id)}}}]
      (testing "has a header"
        (is (test.dom/contains? (main/details state) "Simulator Details")))

      (testing "when given an :http simulator"
        (let [root (main/details state)
              [_ & args] (test.dom/query-one root components/with-status)]
          (testing "renders the component"
            (is (= [http.views/sim {:status ::status :data {:config {:method :http/post :path "/path"} ::and ::things}}]
                   args)))))

      (testing "when given a :ws simulator"
        (let [root (main/details (assoc-in state [:simulators :data id :config :method] :ws))
              [_ & args] (test.dom/query-one root components/with-status)]
          (testing "renders the component"
            (is (= [ws.views/sim {:status ::status :data {:config {:method :ws :path "/path"} ::and ::things}}]
                   args)))))

      (testing "when the simulator has an unknown method"
        (let [root (main/details (update-in state [:simulators :data id :config] assoc :method :unknown))
              [_ component] (test.dom/query-one root components/with-status)]
          (testing "renders a spinner"
            (is (= component components/spinner))))))))

(deftest ^:unit new-test
  (testing "(new)"
    (testing "when type is :ws"
      (let [state {:page       {:query-params {:type "ws"}}
                   :simulators {:status ::status :data ::data}}
            root (main/new state)]
        (testing "renders a header"
          (is (-> root
                  (test.dom/query-one :h2)
                  (test.dom/contains? "New WS Simulator"))))

        (testing "renders the ws create form"
          (is (test.dom/query-one root ws.views/sim-create-form)))))

    (testing "when type is :http"
      (let [state {:page       {:query-params {:type "http"}}
                   :simulators {:status ::status :data ::data}}
            root (main/new state)]
        (testing "renders a header"
          (is (-> root
                  (test.dom/query-one :h2)
                  (test.dom/contains? "New HTTP Simulator"))))

        (testing "renders the http create form"
          (is (test.dom/query-one root http.views/sim-create-form)))))

    (testing "when type is :file"
      (let [state {:page       {:query-params {:type "file"}}
                   :simulators {:status ::status :data ::data}}
            root (main/new state)]
        (testing "renders a header"
          (is (-> root
                  (test.dom/query-one :h2)
                  (test.dom/contains? "New FILE Simulator"))))

        (testing "renders the file create form"
          (is (test.dom/query-one root file.views/sim-create-form)))))

    (testing "when type is any other value"
      (let [state {:page       {:query-params {:type "any"}}
                   :simulators {:status ::status :data ::data}}
            nav-spy (spies/create)]
        (testing "redirects with :http type"
          (with-redefs [nav/nav-and-replace! nav-spy]
            (main/new state)
            (is (spies/called-with? nav-spy :new {:query-params {:type :http}}))))))))

(defn run-tests []
  (t/run-tests))
