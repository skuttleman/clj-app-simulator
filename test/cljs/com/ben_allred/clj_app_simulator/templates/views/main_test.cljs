(ns com.ben-allred.clj-app-simulator.templates.views.main-test
  (:require [clojure.test :as t :refer [deftest testing is] :include-macros true]
            [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.templates.simulators.file.views :as file.views]
            [com.ben-allred.clj-app-simulator.templates.simulators.http.views :as http.views]
            [com.ben-allred.clj-app-simulator.templates.simulators.ws.views :as ws.views]
            [com.ben-allred.clj-app-simulator.templates.components.core :as components]
            [com.ben-allred.clj-app-simulator.templates.views.main :as main]
            [com.ben-allred.clj-app-simulator.templates.views.resources :as resources]
            [com.ben-allred.clj-app-simulator.templates.views.simulators :as sims]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [test.utils.dom :as test.dom]
            [test.utils.spies :as spies]))

(deftest ^:unit header-test
  (testing "(header)"
    (let [path-for-spy (spies/create identity)]
      (with-redefs [nav/path-for path-for-spy]
        (let [header (test.dom/render (main/header {:handler :home}))]
          (testing "has link to home page"
            (let [link (test.dom/query-one header :a.home-link)]
              (is (= :home (:href (test.dom/attrs link))))
              (is (test.dom/query-one link :.logo))))

          (testing "has tabs"
            (let [tabs (test.dom/query-all header :.tab)
                  [_ attrs :as link] (test.dom/query-one (second tabs) :a.tab)
                  home (test.dom/query-one (first tabs) :span.tab)]
              (is (= :resources (:href attrs)))
              (is (test.dom/contains? link "resources"))
              (is (test.dom/contains? home "simulators")))))

        (testing "when on resources page"
          (let [header (test.dom/render (main/header {:handler :resources}))]
            (testing "has resources tabs"
              (let [tabs (test.dom/query-all header :.tab)
                    [_ attrs :as link] (test.dom/query-one (first tabs) :a.tab)
                    resources (test.dom/query-one (second tabs) :span.tab)]
                (is (= :home (:href attrs)))
                (is (test.dom/contains? link "simulators"))
                (is (test.dom/contains? resources "resources"))))))))))

(deftest ^:unit root-test
  (let [path-for-spy (spies/constantly ::nav)
        dispatch-spy (spies/create)
        action-spy (spies/constantly ::action)]
    (with-redefs [nav/path-for path-for-spy
                  store/dispatch dispatch-spy
                  actions/upload action-spy]
      (testing "(root)"
        (let [root (main/home {:simulators    {:status ::status :data ::data}
                               :home-welcome? ::home-welcome?
                               :uploads ::uploads})]
          (testing "has a header"
            (is (= "Simulators" (second (test.dom/query-one root :h2)))))

          (testing "displays simulators"
            (is (= [components/with-status [sims/simulators ::home-welcome? ::uploads] {:status ::status :data ::data}]
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
          (is (-> root
                  (test.dom/query-one components/with-status)
                  (second)
                  (= ws.views/sim-create-form))))))

    (testing "when type is :http"
      (let [state {:page       {:query-params {:type "http"}}
                   :simulators {:status ::status :data ::data}}
            root (main/new state)]
        (testing "renders a header"
          (is (-> root
                  (test.dom/query-one :h2)
                  (test.dom/contains? "New HTTP Simulator"))))

        (testing "renders the http create form"
          (is (-> root
                  (test.dom/query-one components/with-status)
                  (second)
                  (= http.views/sim-create-form))))))

    (testing "when type is :file"
      (let [state {:page       {:query-params {:type "file"}}
                   :simulators {:status ::status :data ::data}}
            root (main/new state)]
        (testing "renders a header"
          (is (-> root
                  (test.dom/query-one :h2)
                  (test.dom/contains? "New FILE Simulator"))))

        (testing "renders the file create form"
          (is (-> root
                  (test.dom/query-one components/with-status)
                  (second)
                  (= file.views/sim-create-form))))))

    (testing "when type is any other value"
      (let [state {:page       {:query-params {:type "any"}}
                   :simulators {:status ::status :data ::data}}
            nav-spy (spies/create)]
        (testing "redirects with :http type"
          (with-redefs [nav/nav-and-replace! nav-spy]
            (main/new state)
            (is (spies/called-with? nav-spy :new {:query-params {:type :http}}))))))))

(deftest ^:unit resources-test
  (testing "(resources)"
    (let [root (main/resources {:uploads-welcome? ::uploads-welcome?
                                :uploads          ::uploads})]
      (testing "has a header"
        (is (test.dom/contains? root "Resources")))

      (testing "renders the root component with status"
        (let [[_ component uploads] (test.dom/query-one root components/with-status)]
          (is (= [resources/root ::uploads-welcome?]
                 component))
          (is (= ::uploads uploads)))))))

(defn run-tests []
  (t/run-tests))
