(ns com.ben-allred.clj-app-simulator.ui.views.main-test
  (:require [cljs.test :as t :refer-macros [deftest testing is]]
            [com.ben-allred.clj-app-simulator.templates.views.core :as views]
            [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.simulators.file.views :as file.views]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.views :as http.views]
            [com.ben-allred.clj-app-simulator.ui.simulators.ws.views :as ws.views]
            [com.ben-allred.clj-app-simulator.ui.views.components.core :as components]
            [com.ben-allred.clj-app-simulator.ui.views.main :as main]
            [com.ben-allred.clj-app-simulator.ui.views.resources :as resources]
            [com.ben-allred.clj-app-simulator.ui.views.simulators :as sims]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [test.utils.dom :as test.dom]
            [test.utils.spies :as spies]))

(deftest ^:unit header-test
  (testing "(header)"
    (testing "renders the shared header"
      (let [component (main/header ::state)]
        (is (= [views/header nav/path-for ::state] component))))))

(deftest ^:unit not-found-test
  (testing "(not-found)"
    (testing "renders the shared not-found"
      (let [component (main/not-found ::state)]
        (is (= [views/not-found nav/path-for ::state] component))))))

(deftest ^:unit root-test
  (let [path-for-spy (spies/constantly ::nav)
        dispatch-spy (spies/create)
        action-spy (spies/constantly ::action)]
    (with-redefs [nav/path-for path-for-spy
                  store/dispatch dispatch-spy
                  actions/upload action-spy]
      (testing "(root)"
        (let [root (-> {:simulators    {:status ::status :data ::data}
                        :home-welcome? ::home-welcome?}
                       (main/root)
                       (test.dom/query-one views/root))]
          (testing "has a create menu"
            (let [[_ attrs element] (test.dom/query-one root components/menu)
                  items (set (:items attrs))]
              (is (contains? items {:href ::nav :label "HTTP Simulator"}))
              (is (contains? items {:href ::nav :label "WS Simulator"}))
              (is (spies/called-with? path-for-spy :new {:query-params {:type :http}}))
              (is (spies/called-with? path-for-spy :new {:query-params {:type :ws}}))
              (is (-> element
                      (test.dom/query-one :.button-success)
                      (test.dom/contains? "Create")))))

          (testing "displays simulators"
            (is (-> root
                    (test.dom/query-one components/with-status)
                    (= [components/with-status [sims/simulators ::home-welcome?] {:status ::status :data ::data}])))))

        (testing "when there are uploads"
          (spies/reset! path-for-spy)
          (let [root (-> {:simulators    {:status ::status :data ::data}
                          :home-welcome? ::home-welcome?
                          :uploads       {:data [::file-1 ::file-2]}}
                         (main/root)
                         (test.dom/query-one views/root))]
            (testing "has file option in the create menu"
              (let [items (-> root
                              (test.dom/query-one components/menu)
                              (test.dom/attrs)
                              (:items)
                              (set))]
                (is (contains? items {:href ::nav :label "File Server"}))
                (is (spies/called-with? path-for-spy :new {:query-params {:type :file}}))))))))))

(deftest ^:unit details-test
  (testing "(details)"
    (let [id #uuid "ed206e4f-2eaf-4c3e-aa1e-720be417be51"
          state {:simulators {:status ::status
                              :data   {id {:config {:method :http/post :path "/path"}
                                           ::and   ::things}}}
                 :page       {:route-params {:id (str id)}}}]
      (testing "when given an :http simulator"
        (let [root (-> state
                       (main/details)
                       (test.dom/query-one views/details))
              [_ & args] (test.dom/query-one root components/with-status)]
          (testing "renders the component"
            (is (= [http.views/sim {:status ::status :data {:config {:method :http/post :path "/path"} ::and ::things}}]
                   args)))))

      (testing "when given a :ws simulator"
        (let [root (-> state
                       (assoc-in [:simulators :data id :config :method] :ws)
                       (main/details)
                       (test.dom/query-one views/details))
              [_ & args] (test.dom/query-one root components/with-status)]
          (testing "renders the component"
            (is (= [ws.views/sim {:status ::status :data {:config {:method :ws :path "/path"} ::and ::things}}]
                   args)))))

      (testing "when the simulator has an unknown method"
        (let [root (-> state
                       (update-in [:simulators :data id :config] assoc :method :unknown)
                       (main/details)
                       (test.dom/query-one views/details))
              [_ component] (test.dom/query-one root components/with-status)]
          (testing "renders a spinner"
            (is (= component views/spinner))))))))

(deftest ^:unit new-test
  (testing "(new)"
    (testing "when type is :ws"
      (let [state {:page       {:query-params {:type "ws"}}
                   :simulators {:status ::status :data ::data}}
            root (-> state
                     (main/new)
                     (test.dom/query-one views/new))]
        (testing "renders the ws create form"
          (is (test.dom/query-one root ws.views/sim-create-form)))))

    (testing "when type is :http"
      (let [state {:page       {:query-params {:type "http"}}
                   :simulators {:status ::status :data ::data}}
            root (-> state
                     (main/new)
                     (test.dom/query-one views/new))]
        (testing "renders the http create form"
          (is (test.dom/query-one root http.views/sim-create-form)))))

    (testing "when type is :file"
      (let [state {:page       {:query-params {:type "file"}}
                   :simulators {:status ::status :data ::data}}
            root (-> state
                     (main/new)
                     (test.dom/query-one views/new))]
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

(deftest ^:unit resources-test
  (testing "(resources)"
    (let [root (-> {:uploads-welcome? ::uploads-welcome?
                    :uploads          ::uploads}
                   (main/resources)
                   (test.dom/query-one views/resources))]
      (testing "renders the root component with status"
        (let [[_ component uploads] (test.dom/query-one root components/with-status)]
          (is (= [resources/root ::uploads-welcome?]
                 component))
          (is (= ::uploads uploads)))))))

(defn run-tests []
  (t/run-tests))
