(ns com.ben-allred.clj-app-simulator.api.services.simulators.routes-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.api.services.simulators.routes :as routes.sim]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
            [compojure.core :as c]
            [com.ben-allred.clj-app-simulator.api.services.activity :as activity]
            [com.ben-allred.clj-app-simulator.services.http :as http]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.api.utils.respond :as respond])
  (:import [java.io ByteArrayInputStream]))

(defn ^:private find-by-method-and-path [routes method path]
  (->> routes
       (keep (fn [[m p f]]
               (when (and (= m method) (= p path))
                 f)))
       (first)))

(defn ^:private as-stream [s]
  (-> s
      (.getBytes "UTF-8")
      (ByteArrayInputStream.)))

(deftest ^:unit sim-route-test
  (testing "(sim-route)"
    (let [receive-spy (spies/create (constantly ::response))
          publish-spy (spies/create)
          details-spy (spies/create (constantly {:id       ::id
                                                 :config   ::config
                                                 :details  ::details
                                                 :requests ::requests}))
          requests-spy (spies/create (constantly [::request]))]
      (with-redefs [common/receive receive-spy
                    activity/publish publish-spy
                    common/details details-spy
                    common/requests requests-spy]
        (let [sim (routes.sim/sim-route ::simulator)]
          (testing "receives the request"
            (spies/reset! receive-spy)
            (let [request {:body ::body :data ::data}]
              (sim request)
              (is (spies/called-with? receive-spy ::simulator request))))

          (testing "when the request :body is an InputStream"
            (testing "slurps and trims the :body"
              (spies/reset! receive-spy)
              (let [request {:body (as-stream "   a body\t")}]
                (sim request)
                (is (spies/called-with? receive-spy ::simulator {:body "a body"}))))

            (testing "and when the trimmed :body is empty"
              (testing "stores the body as nil"
                (spies/reset! receive-spy)
                (let [request {:body (as-stream "\n  ")}]
                  (sim request)
                  (is (nil? (:body (last (first (spies/calls receive-spy))))))))))

          (testing "publishes an event"
            (spies/reset! publish-spy details-spy requests-spy)
            (sim {::a ::request})
            (is (spies/called-with? details-spy ::simulator))
            (is (spies/called-with? requests-spy ::simulator))
            (is (spies/called-with? publish-spy
                                    :simulators/receive
                                    {:simulator {:id ::id :config ::config}
                                     :request   ::request})))

          (testing "returns the response"
            (is (= ::response (sim {::a ::request})))))))))

(deftest ^:unit get-sim-test
  (testing "(get-sim)"
    (let [details-spy (spies/create (constantly ::details))
          respond-spy (spies/create (constantly ::response))]
      (with-redefs [common/details details-spy
                    respond/with respond-spy]
        (let [handler (routes.sim/get-sim ::simulator)
              result (handler ::request)]
          (testing "responds with the simulator's details"
            (is (spies/called-with? details-spy ::simulator))
            (is (spies/called-with? respond-spy [:ok {:simulator ::details}]))
            (is (= ::response result))))))))

(deftest ^:unit delete-sim-test
  (testing "(delete-sim)"
    (let [details-spy (spies/create (constantly {:id       ::id
                                                 :config   ::config
                                                 :details  ::details
                                                 :requests ::requests}))
          publish-spy (spies/create)
          delete-spy (spies/create)
          respond-spy (spies/create (constantly ::response))]
      (with-redefs [common/details details-spy
                    activity/publish publish-spy
                    respond/with respond-spy]
        (testing "deletes the simulator"
          (let [handler (routes.sim/delete-sim ::simulator delete-spy)
                result (handler ::request)]
            (is (spies/called-with? publish-spy :simulators/delete
                                    {:id ::id :config ::config}))
            (is (spies/called? delete-spy))
            (is (spies/called-with? respond-spy [:no-content]))
            (is (= result ::response))))))))

(deftest ^:unit patch-sim-test
  (testing "(patch-sim)"
    (let [[reset-spy reset-requests-spy reset-response-spy change-spy publish-spy] (repeatedly spies/create)
          details-spy (spies/create (constantly ::details))
          respond-spy (spies/create (constantly ::response))]
      (with-redefs [common/reset reset-spy
                    common/reset-requests reset-requests-spy
                    common/reset-response reset-response-spy
                    common/change change-spy
                    common/details details-spy
                    activity/publish publish-spy
                    respond/with respond-spy]
        (let [handler (routes.sim/patch-sim ::simulator)]
          (testing "when resetting the simulator"
            (spies/reset! reset-spy details-spy publish-spy respond-spy)
            (let [result (handler {:body {:action :simulators/reset}})]
              (testing "takes the requested action"
                (is (spies/called-with? reset-spy ::simulator)))

              (testing "gets the details"
                (is (spies/called-with? details-spy ::simulator)))

              (testing "publishes the event"
                (is (spies/called-with? publish-spy :simulators/reset ::details)))

              (testing "responds with the details"
                (is (spies/called-with? respond-spy [:ok ::details]))
                (is (= ::response result)))))

          (testing "when resetting the requests"
            (spies/reset! reset-requests-spy details-spy publish-spy respond-spy)
            (let [result (handler {:body {:action :http/reset-requests}})]
              (testing "takes the requested action"
                (is (spies/called-with? reset-requests-spy ::simulator)))

              (testing "gets the details"
                (is (spies/called-with? details-spy ::simulator)))

              (testing "publishes the event"
                (is (spies/called-with? publish-spy :http/reset-requests ::details)))

              (testing "responds with the details"
                (is (spies/called-with? respond-spy [:ok ::details]))
                (is (= ::response result)))))

          (testing "when resetting the response"
            (spies/reset! reset-response-spy details-spy publish-spy respond-spy)
            (let [result (handler {:body {:action :http/reset-response}})]
              (testing "takes the requested action"
                (is (spies/called-with? reset-response-spy ::simulator)))

              (testing "gets the details"
                (is (spies/called-with? details-spy ::simulator)))

              (testing "publishes the event"
                (is (spies/called-with? publish-spy :http/reset-response ::details)))

              (testing "responds with the details"
                (is (spies/called-with? respond-spy [:ok ::details]))
                (is (= ::response result)))))

          (testing "when changing the simulator"
            (spies/reset! change-spy details-spy publish-spy respond-spy)
            (let [result (handler {:body {:action :http/change :config ::config}})]
              (testing "takes the requested action"
                (is (spies/called-with? change-spy ::simulator ::config)))

              (testing "gets the details"
                (is (spies/called-with? details-spy ::simulator)))

              (testing "publishes the event"
                (is (spies/called-with? publish-spy :http/change ::details)))

              (testing "responds with the details"
                (is (spies/called-with? respond-spy [:ok ::details]))
                (is (= ::response result))))

            (testing "and when the change fails"
              (spies/reset! change-spy respond-spy)
              (spies/respond-with! change-spy (fn [_ _]
                                                (throw (ex-info "An Exception" {:problems ::problems}))))
              (let [result (handler {:body {:action :http/change :config ::config}})]
                (testing "responds with error"
                  (is (spies/called-with? respond-spy [:bad-request ::problems]))
                  (is (= ::response result))))))

          (testing "when an unknown action is patched"
            (spies/reset! publish-spy)
            (handler {:body {:action :unknown}})
            (testing "does not publish an action"
              (spies/never-called? publish-spy)))

          (testing "when the action is a string"
            (spies/reset! reset-spy publish-spy)
            (handler {:body {:action "simulators/reset"}})
            (testing "converts the keyword to a string"
              (is (spies/called-with? reset-spy ::simulator))
              (is (spies/called-with? publish-spy :simulators/reset ::details)))))))))

(deftest ^:unit routes-test
  (testing "(routes)"
    (let [details-spy (spies/create (constantly {:config {:method ::method :path "/some/path"}
                                                 :id     "some-id"}))
          get-sim-spy (spies/create (constantly ::get-sim))
          delete-sim-spy (spies/create (constantly ::delete-sim))
          patch-sim-spy (spies/create (constantly ::patch-sim))
          sim-route-spy (spies/create (constantly ::sim-route))
          delete! (spies/create (constantly ::delete!))]
      (with-redefs [common/details details-spy
                    routes.sim/get-sim get-sim-spy
                    routes.sim/delete-sim delete-sim-spy
                    routes.sim/patch-sim patch-sim-spy
                    routes.sim/sim-route sim-route-spy]
        (testing "contains a sim route"
          (spies/reset! details-spy sim-route-spy)
          (is (-> ::simulator
                  (routes.sim/routes delete!)
                  (find-by-method-and-path :method "/simulators/some/path")
                  (= ::sim-route)))
          (is (spies/called-with? sim-route-spy ::simulator)))

        (testing "when the path is /"
          (spies/respond-with! details-spy (constantly {:config {:method ::method :path "/"}
                                                        :id     "some-id"}))
          (testing "drops the trailing slash on the sim route"
            (is (-> ::simulator
                    (routes.sim/routes delete!)
                    (find-by-method-and-path :method "/simulators")
                    (= ::sim-route)))))

        (testing "contains routes to get the simulator"
          (spies/reset! details-spy get-sim-spy)
          (let [sims (routes.sim/routes ::simulator delete!)]
            (is (spies/called-with? get-sim-spy ::simulator))
            (is (= ::get-sim
                   (find-by-method-and-path sims :get "/api/simulators/method/some/path")))
            (is (= ::get-sim
                   (find-by-method-and-path sims :get "/api/simulators/some-id")))))

        (testing "when building routes to delete the simulator"
          (spies/reset! details-spy delete-sim-spy delete!)
          (let [sims (routes.sim/routes ::simulator delete!)]
            (testing "has the routes"
              (is (= ::delete-sim
                     (find-by-method-and-path sims :delete "/api/simulators/method/some/path")))
              (is (= ::delete-sim
                     (find-by-method-and-path sims :delete "/api/simulators/some-id"))))

            (testing "has a function to delete the simulator"
              (let [delete-fn! (last (first (spies/calls delete-sim-spy)))]
                (is (spies/called-with? delete-sim-spy ::simulator (spies/matcher fn?)))
                (delete-fn!)
                (is (spies/called-with? delete! ::method "/some/path"))))))

        (testing "contains routes to update the simulator"
          (spies/reset! details-spy patch-sim-spy)
          (let [sims (routes.sim/routes ::simulator delete!)]
            (is (spies/called-with? patch-sim-spy ::simulator))
            (is (= ::patch-sim
                   (find-by-method-and-path sims :patch "/api/simulators/method/some/path")))
            (is (= ::patch-sim
                   (find-by-method-and-path sims :patch "/api/simulators/some-id")))))))))

(deftest ^:unit http-sim->routes-test
  (testing "(http-sim->routes)"
    (let [routes-spy (spies/create (constantly [[::route ::1] [::route ::2]]))
          make-route-spy (spies/create (comp second vector))]
      (with-redefs [routes.sim/routes routes-spy
                    c/make-route make-route-spy]
        (testing "make routes"
          (let [routes (routes.sim/http-sim->routes ::simulator ::delete!)]
            (is (= [::1 ::2] routes))
            (is (spies/called-with? routes-spy ::simulator ::delete!))
            (is (spies/called-with? make-route-spy ::route ::1))
            (is (spies/called-with? make-route-spy ::route ::2))))))))
