(ns com.ben-allred.app-simulator.ui.views.main-test
  (:require
    [clojure.test :as t :refer-macros [deftest is testing]]
    [com.ben-allred.app-simulator.templates.views.core :as views]
    [com.ben-allred.app-simulator.templates.views.forms.file :as file.views]
    [com.ben-allred.app-simulator.templates.views.forms.http :as http.views]
    [com.ben-allred.app-simulator.templates.views.forms.ws :as ws.views]
    [com.ben-allred.app-simulator.templates.views.simulators :as views.sim]
    [com.ben-allred.app-simulator.ui.services.navigation :as nav]
    [com.ben-allred.app-simulator.ui.services.store.actions :as actions]
    [com.ben-allred.app-simulator.ui.services.store.core :as store]
    [com.ben-allred.app-simulator.ui.views.components.core :as components]
    [com.ben-allred.app-simulator.ui.views.main :as main]
    [com.ben-allred.app-simulator.ui.views.resources :as resources]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [test.utils.dom :as test.dom]
    [test.utils.spies :as spies]))

(deftest ^:unit root-test
  (with-redefs [nav/path-for (spies/constantly ::nav)
                store/dispatch (constantly nil)
                actions/upload (constantly ::action)]
    (testing "(root)"
      (let [root (-> {:simulators {:status ::status :data ::data}}
                     (main/root)
                     (test.dom/query-one views/root))]
        (testing "has a create menu"
          (let [[_ attrs element] (test.dom/query-one root components/menu)
                items (set (:items attrs))]
            (is (contains? items {:href ::nav :label "HTTP Simulator"}))
            (is (contains? items {:href ::nav :label "WS Simulator"}))
            (is (spies/called-with? nav/path-for :new {:query-params {:type :http}}))
            (is (spies/called-with? nav/path-for :new {:query-params {:type :ws}}))
            (is (-> element
                    (test.dom/query-one :.button)
                    (test.dom/contains? "Create")))))

        (testing "displays simulators"
          (is (-> root
                  (test.dom/query-one views.sim/simulators)
                  (= [views.sim/simulators ::data])))))

      (testing "when there are uploads"
        (spies/reset! nav/path-for)
        (let [root (-> {:simulators {:status ::status :data ::data}
                        :resources  {:data [::file-1 ::file-2]}}
                       (main/root)
                       (test.dom/query-one views/root))]
          (testing "has file option in the create menu"
            (let [items (-> root
                            (test.dom/query-one components/menu)
                            (test.dom/attrs)
                            (:items)
                            (set))]
              (is (contains? items {:href ::nav :label "File Server"}))
              (is (spies/called-with? nav/path-for :new {:query-params {:type :file}})))))))))

(deftest ^:unit details-test
  (testing "(details)"
    (let [id #uuid "ed206e4f-2eaf-4c3e-aa1e-720be417be51"
          state {:simulators {:status ::status
                              :data   {id {:config {:method :http/post :path "/path"}
                                           ::and   ::things}}}
                 :page       {:route-params {:id id}}}]
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
                       (assoc-in [:simulators :data id :config :method] :ws/ws)
                       (main/details)
                       (test.dom/query-one views/details))
              [_ & args] (test.dom/query-one root components/with-status)]
          (testing "renders the component"
            (is (= [ws.views/sim {:status ::status :data {:config {:method :ws/ws :path "/path"} ::and ::things}}]
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
                   :simulators {:status ::status :data ::data}}]
        (testing "redirects with :http type"
          (with-redefs [nav/nav-and-replace! (spies/create)]
            (main/new state)
            (is (spies/called-with? nav/nav-and-replace! :new {:query-params {:type :http}}))))))))

(deftest ^:unit resources-test
  (testing "(resources)"
    (let [root (-> {:resources ::resources}
                   (main/resources)
                   (test.dom/query-one views/resources))]
      (testing "renders the root component with status"
        (let [[_ component resources] (test.dom/query-one root components/with-status)]
          (is (= resources/root
                 component))
          (is (= ::resources resources)))))))

(defn run-tests []
  (t/run-tests))
