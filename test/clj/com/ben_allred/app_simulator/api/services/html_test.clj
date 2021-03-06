(ns com.ben-allred.app-simulator.api.services.html-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [com.ben-allred.app-simulator.api.services.html :as html]
    [com.ben-allred.app-simulator.api.services.resources.core :as resources]
    [com.ben-allred.app-simulator.api.services.simulators.core :as simulators]
    [com.ben-allred.app-simulator.services.navigation :as nav*]
    [com.ben-allred.app-simulator.services.ui-reducers :as ui-reducers]
    [com.ben-allred.app-simulator.templates.core :as templates]
    [com.ben-allred.app-simulator.templates.views.core :as views]
    [com.ben-allred.app-simulator.templates.views.forms.file :as file.views]
    [com.ben-allred.app-simulator.templates.views.forms.http :as http.views]
    [com.ben-allred.app-simulator.templates.views.forms.ws :as ws.views]
    [com.ben-allred.app-simulator.templates.views.resources :as views.res]
    [com.ben-allred.app-simulator.templates.views.simulators :as views.sim]
    [com.ben-allred.app-simulator.utils.simulators :as utils.sims]
    [com.ben-allred.app-simulator.utils.uuids :as uuids]
    [com.ben-allred.collaj.core :as collaj]
    [test.utils.dom :as test.dom]
    [test.utils.spies :as spies]))

(deftest ^:unit app-test
  (testing "(app)"
    (let [[component attrs state] (html/app ::state)]
      (testing "renders the app"
        (is (= component views/app*))
        (is (= state ::state)))

      (testing "when rendering components"
        (let [{new* :new :keys [home details resources]} (:components attrs)]
          (testing "renders a home component"
            (let [tree (-> {:simulators {:data ::simulators}}
                           (home)
                           (test.dom/query-one views/root))]
              (is (-> tree
                      (test.dom/query-one :.create-button)
                      (test.dom/attrs)
                      (:disabled)))
              (is (-> tree
                      (test.dom/query-one views.sim/simulators)
                      (second)
                      (= ::simulators)))
              (is (test.dom/contains? tree [views/spinner]))))

          (testing "when rendering a new component"
            (testing "and when the type is ws"
              (let [state {:page {:query-params {:type "ws"}}}
                    args (-> state
                             (new*)
                             (test.dom/query-one views/new)
                             (rest))]
                (testing "renders the ws create form"
                  (is (= args
                         [state [ws.views/sim-create-form] [views/spinner]])))))

            (testing "and when the type is file"
              (let [state {:page    {:query-params {:type "file"}}
                           :resources {:data ::resources}}
                    args (-> state
                             (new*)
                             (test.dom/query-one views/new)
                             (rest))]
                (testing "renders the file create form"
                  (is (= args
                         [state [file.views/sim-create-form ::resources] [views/spinner]])))))

            (testing "and when there is any other type"
              (let [state {:page {:query-params {:type ::type}}}
                    args (-> state
                             (new*)
                             (test.dom/query-one views/new)
                             (rest))]
                (testing "renders the http create form"
                  (is (= args
                         [state [http.views/sim-create-form] [views/spinner]]))))))

          (testing "when rendering a details component"
            (with-redefs [utils.sims/config->section (spies/create)]
              (let [simulator {:config ::config :data ::data}
                    state {:page       {:route-params {:id ::id}}
                           :simulators {:data {::id simulator}}
                           :resources  {:data ::resources}}]
                (testing "and when the simulator is type :http"
                  (spies/respond-with! utils.sims/config->section (constantly "http"))
                  (testing "renders the view"
                    (let [args (-> state
                                   (details)
                                   (test.dom/query-one views/details)
                                   (rest))]
                      (is (spies/called-with? utils.sims/config->section ::config))
                      (is (= args
                             [[http.views/sim simulator] [views/spinner]])))))

                (testing "and when the simulator is type :ws"
                  (spies/respond-with! utils.sims/config->section (constantly "ws"))
                  (testing "renders the view"
                    (let [args (-> state
                                   (details)
                                   (test.dom/query-one views/details)
                                   (rest))]
                      (is (spies/called-with? utils.sims/config->section ::config))
                      (is (= args
                             [[ws.views/sim simulator] [views/spinner]])))))

                (testing "and when the simulator is type :file"
                  (spies/respond-with! utils.sims/config->section (constantly "file"))
                  (testing "renders the view"
                    (let [args (-> state
                                   (details)
                                   (test.dom/query-one views/details)
                                   (rest))]
                      (is (spies/called-with? utils.sims/config->section ::config))
                      (is (= args
                             [[file.views/sim simulator ::resources] [views/spinner]]))))))

              (testing "and when the simulator is not found"
                (let [[msg spinner] (-> state
                                        (details)
                                        (test.dom/query-one views/details)
                                        (rest))]
                  (testing "renders a message"
                    (is (test.dom/re-contains? msg #"simulator"))
                    (is (test.dom/re-contains? msg #"not"))
                    (is (test.dom/re-contains? msg #"found"))
                    (is (= spinner [views/spinner])))))))

          (testing "when rendering a resources component"
            (let [state {:resources {:data ::data}}
                  [resources' delete-attrs btn resource data spinner]
                  (-> state
                      (resources)
                      (test.dom/query-one views/resources)
                      (second))]
              (testing "calls the resources component with correct args"
                (is (= resources' views.res/resources))
                (is (:disabled delete-attrs))
                (is (-> btn
                        (test.dom/query-one :.button)
                        (test.dom/attrs)
                        (:disabled)))
                (is (= data ::data))
                (is (= spinner [views/spinner])))

              (testing "renders with the resource"
                (let [[attrs upload btn] (-> ::upload
                                             (resource)
                                             (test.dom/query-one views.res/resource)
                                             (rest))]
                  (is (:disabled attrs))
                  (is (= upload ::upload))
                  (is (-> btn
                          (test.dom/query-one :.button)
                          (test.dom/attrs)
                          (:disabled)))
                  (is (test.dom/contains? btn "Replace"))))))))

      (testing "renders toasts"
        (is (-> ::state
                ((:toast attrs))
                (test.dom/query-one :.toast-container))))

      (testing "renders the modal"
        (is (-> ::state
                ((:modal attrs))
                (test.dom/query-one :.modal-wrapper.unmounted)))))))

(deftest ^:unit tree->html-test
  (testing "(tree->html)"
    (let [hiccup-spy (spies/constantly "[HTML]")]
      (with-redefs [html/hiccup hiccup-spy]
        (let [html (html/tree->html ::tree)]
          (testing "converts tree to html"
            (is (spies/called-with? hiccup-spy ::tree)))

          (testing "prepends html DOCTYPE"
            (is (= "<!DOCTYPE html>[HTML]" html))))))))

(deftest ^:unit build-tree-test
  (testing "(build-tree)"
    (testing "when building the template"
      (let [tree (html/build-tree nil)]
        (testing "links to main.css"
          (let [attrs (->> (test.dom/query-all tree :link)
                           (map test.dom/attrs)
                           (filter (comp #{"/css/main.css"} :href))
                           (first))]
            (is (= "text/css" (:type attrs)))
            (is (= "stylesheet" (:rel attrs)))))

        (testing "uses app.js"
          (let [attrs (->> (test.dom/query-all tree :script)
                           (map test.dom/attrs)
                           (filter (comp #{"/js/compiled/app.js"} :src))
                           (first))]
            (is (= "text/javascript" (:type attrs)))))

        (testing "mounts the app"
          (is (->> (test.dom/query-all tree :script)
                   (filter #(test.dom/re-contains? % #"com\.ben_allred\.app_simulator\.ui\.app\.mount_BANG_\(\)"))
                   (first))))))

    (testing "when including content"
      (with-redefs [templates/render (spies/constantly ::rendered)]
        (let [tree (html/build-tree ::content)]
          (testing "renders content"
            (is (spies/called-with? templates/render ::content)))

          (testing "embeds content in #app"
            (is (-> tree
                    (test.dom/query-one :#app)
                    (test.dom/contains? ::rendered)))))))))

(deftest ^:unit hydrate-test
  (testing "(hydrate)"
    (let [dispatch-spy (spies/create)
          get-state-spy (spies/constantly {:some :state})]
      (with-redefs [html/build-tree (spies/constantly ::tree)
                    html/tree->html (spies/constantly ::html)
                    html/app (spies/constantly ::app)
                    collaj/create-store (spies/constantly {:dispatch dispatch-spy :get-state get-state-spy})
                    resources/list-files (spies/constantly ::resources)
                    simulators/details (spies/constantly [::sim-1 ::sim-2 ::sim-3])]
        (let [html (html/hydrate ::page ::env)
              state (ffirst (spies/calls html/app))]
          (testing "gets the default state"
            (is (spies/called-with? collaj/create-store ui-reducers/root))
            (is (spies/called-with? get-state-spy)))

          (testing "gets current resources"
            (is (spies/called-with? resources/list-files ::env)))

          (testing "gets current simulators"
            (is (spies/called-with? simulators/details ::env)))

          (testing "calls dispatch to setup state"
            (is (spies/called-with? dispatch-spy [:files.fetch-all/succeed {:resources ::resources}]))
            (is (spies/called-with-times? dispatch-spy 1 [:simulators/clear]))
            (is (-> dispatch-spy
                    (spies/with-order [:simulators/clear])
                    (spies/with-order [:simulators.fetch-one/succeed {:simulator ::sim-1}])
                    (spies/ordered-calls?)))
            (is (-> dispatch-spy
                    (spies/with-order [:simulators/clear])
                    (spies/with-order [:simulators.fetch-one/succeed {:simulator ::sim-2}])
                    (spies/ordered-calls?)))
            (is (-> dispatch-spy
                    (spies/with-order [:simulators/clear])
                    (spies/with-order [:simulators.fetch-one/succeed {:simulator ::sim-3}])
                    (spies/ordered-calls?))))

          (testing "generates content"
            (is (= ::page (:page state)))
            (is (= :state (:some state))))

          (testing "builds the dom tree"
            (is (spies/called-with? html/build-tree ::app)))

          (testing "converts the tree to html"
            (is (spies/called-with? html/tree->html ::tree))
            (is (= ::html html))))))))

(deftest ^:unit render-test
  (testing "(render)"
    (with-redefs [nav*/match-route (spies/constantly ::page)
                  html/hydrate (spies/constantly ::html)]
      (testing "returns html"
        (let [result (html/render {:uri "/any/ole/route"} ::env)]
          (is (spies/called-with? nav*/match-route "/any/ole/route"))
          (is (spies/called-with? html/hydrate ::page ::env))
          (is (= ::html result))))

      (testing "when the request has a query-string"
        (spies/reset! nav*/match-route)
        (html/render {:uri "/any/ole/route" :query-string "a=b"} ::env)

        (is (spies/called-with? nav*/match-route "/any/ole/route?a=b"))))))
