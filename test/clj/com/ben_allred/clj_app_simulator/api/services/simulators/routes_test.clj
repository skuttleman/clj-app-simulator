(ns com.ben-allred.clj-app-simulator.api.services.simulators.routes-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.api.services.simulators.routes :as routes.sim]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
            [compojure.core :as c]
            [com.ben-allred.clj-app-simulator.api.services.activity :as activity]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.utils.uuids :as uuids]
            [integration.utils.http :as test.http]
            [com.ben-allred.clj-app-simulator.api.services.simulators.simulators :as sims]
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

(deftest ^:unit receive-test
  (testing "(receive)"
    (let [publish-spy (spies/create)
          details-spy (spies/constantly {:id ::id :config ::config :other ::things})
          request-spy (spies/constantly [::request-1 ::request-2])]
      (with-redefs [activity/publish publish-spy
                    common/details details-spy
                    common/requests request-spy]
        (testing "publishes an event"
          (routes.sim/receive ::env ::simulator {::extra ::things})
          (is (spies/called-with? details-spy ::simulator))
          (is (spies/called-with? request-spy ::simulator))
          (is (spies/called-with? publish-spy
                                  ::env
                                  :simulators/receive
                                  {:simulator {:id ::id :config ::config}
                                   :request ::request-2
                                   ::extra ::things})))))))

(deftest ^:unit http-sim-route-test
  (testing "(http-sim-route)"
    (let [receive-spy (spies/constantly ::response)
          publish-spy (spies/create)
          receive-publish-spy (spies/create)]
      (with-redefs [common/receive receive-spy
                    activity/publish publish-spy
                    routes.sim/receive receive-publish-spy]
        (let [sim (routes.sim/http-sim-route ::env ::simulator)]
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
            (spies/reset! receive-publish-spy)
            (sim {::a ::request})
            (is (spies/called-with? receive-publish-spy ::env ::simulator {})))

          (testing "returns the response"
            (is (= ::response (sim {::a ::request})))))))))

(deftest ^:unit ws-sim-route-test
  (testing "(ws-sim-route)"
    (let [connect-spy (spies/constantly ::socket-upgrade)]
      (with-redefs [common/connect connect-spy]
        (let [result ((routes.sim/ws-sim-route ::simulator) ::request)]
          (testing "connects the socket request"
            (is (spies/called-with? connect-spy ::simulator ::request)))

          (testing "returns the socket upgrade"
            (is (= ::socket-upgrade result))))))))

(deftest ^:unit get-sim-test
  (testing "(get-sim)"
    (let [details-spy (spies/constantly ::details)]
      (with-redefs [common/details details-spy]
        (let [handler (routes.sim/get-sim ::simulator)
              result (handler ::request)]
          (testing "responds with the simulator's details"
            (is (spies/called-with? details-spy ::simulator))
            (is (= [:ok {:simulator ::details}] result))))))))

(deftest ^:unit delete-sim-test
  (testing "(delete-sim)"
    (let [details-spy (spies/constantly {:id       ::id
                                                  :config   ::config
                                                  :details  ::details
                                                  :requests ::requests})
          identifier-spy (spies/constantly ::identifier)
          publish-spy (spies/create)
          delete-spy (spies/create)]
      (with-redefs [common/details details-spy
                    common/identifier identifier-spy
                    activity/publish publish-spy]
        (testing "deletes the simulator"
          (let [handler (routes.sim/delete-sim ::env ::simulator delete-spy)
                result (handler ::request)]
            (is (spies/called-with? publish-spy
                                    ::env
                                    :simulators/delete
                                    {:id ::id :config ::config}))

            (is (spies/called-with? identifier-spy ::simulator))
            (is (spies/called-with? delete-spy ::identifier))
            (is (= result [:no-content]))))))))

(deftest ^:unit patch-sim-test
  (testing "(patch-sim)"
    (let [[reset-spy reset-requests-spy reset-response-spy change-spy publish-spy] (repeatedly spies/create)
          details-spy (spies/constantly ::details)]
      (with-redefs [common/reset reset-spy
                    common/reset-requests reset-requests-spy
                    common/reset-response reset-response-spy
                    common/change change-spy
                    common/details details-spy
                    activity/publish publish-spy]
        (let [handler (routes.sim/patch-sim ::env ::simulator)]
          (testing "when resetting the simulator"
            (spies/reset! reset-spy details-spy publish-spy)
            (let [result (handler {:body {:action :simulators/reset}})]
              (testing "takes the requested action"
                (is (spies/called-with? reset-spy ::simulator)))

              (testing "gets the details"
                (is (spies/called-with? details-spy ::simulator)))

              (testing "publishes the event"
                (is (spies/called-with? publish-spy ::env :simulators/reset ::details)))

              (testing "responds with the details"
                (is (= [:ok ::details] result)))))

          (testing "when changing the simulator"
            (spies/reset! change-spy details-spy publish-spy)
            (let [result (handler {:body {:action :simulators/change :config ::config}})]
              (testing "takes the requested action"
                (is (spies/called-with? change-spy ::simulator ::config)))

              (testing "gets the details"
                (is (spies/called-with? details-spy ::simulator)))

              (testing "publishes the event"
                (is (spies/called-with? publish-spy ::env :simulators/change ::details)))

              (testing "responds with the details"
                (is (= [:ok ::details] result))))

            (testing "and when the change fails"
              (spies/reset! change-spy)
              (spies/respond-with! change-spy (fn [_ _]
                                                (throw (ex-info "An Exception" {:problems ::problems}))))
              (let [result (handler {:body {:action :simulators/change :config ::config}})]
                (testing "responds with error"
                  (is (= [:bad-request ::problems] result))))))

          (testing "when resetting the requests"
            (spies/reset! reset-requests-spy details-spy publish-spy)
            (let [result (handler {:body {:action :simulators.http/reset-requests}})]
              (testing "takes the requested action"
                (is (spies/called-with? reset-requests-spy ::simulator)))

              (testing "gets the details"
                (is (spies/called-with? details-spy ::simulator)))

              (testing "publishes the event"
                (is (spies/called-with? publish-spy ::env :simulators.http/reset-requests ::details)))

              (testing "responds with the details"
                (is (= [:ok ::details] result)))))

          (testing "when resetting the response"
            (spies/reset! reset-response-spy details-spy publish-spy)
            (let [result (handler {:body {:action :simulators.http/reset-response}})]
              (testing "takes the requested action"
                (is (spies/called-with? reset-response-spy ::simulator)))

              (testing "gets the details"
                (is (spies/called-with? details-spy ::simulator)))

              (testing "publishes the event"
                (is (spies/called-with? publish-spy ::env :simulators.http/reset-response ::details)))

              (testing "responds with the details"
                (is (= [:ok ::details] result)))))

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
              (is (spies/called-with? publish-spy ::env :simulators/reset ::details)))))))))

(deftest ^:unit patch-ws-test
  (testing "(patch-ws)"
    (let [reset-spy (spies/create)
          reset-messages-spy (spies/create)
          change-spy (spies/create)
          disconnect-spy (spies/create)
          details-spy (spies/constantly {::some ::details})
          publish-spy (spies/create)]
      (with-redefs [common/reset reset-spy
                    common/reset-messages reset-messages-spy
                    common/change change-spy
                    common/disconnect disconnect-spy
                    common/details details-spy
                    activity/publish publish-spy]
        (let [handler (routes.sim/patch-ws ::env ::simulator)]
          (testing "when resetting the simulator"
            (spies/reset! reset-spy details-spy publish-spy)
            (let [result (handler {:body {:action :simulators/reset}})]
              (testing "takes the requested action"
                (is (spies/called-with? reset-spy ::simulator)))

              (testing "gets the details"
                (is (spies/called-with? details-spy ::simulator)))

              (testing "publishes the event"
                (is (spies/called-with? publish-spy ::env :simulators/reset {::some ::details})))

              (testing "responds with the details"
                (is (= [:ok {::some ::details}] result)))))

          (testing "when changing the simulator"
            (spies/reset! change-spy details-spy publish-spy)
            (let [result (handler {:body {:action :simulators/change :config ::config}})]
              (testing "takes the requested action"
                (is (spies/called-with? change-spy ::simulator ::config)))

              (testing "gets the details"
                (is (spies/called-with? details-spy ::simulator)))

              (testing "publishes the event"
                (is (spies/called-with? publish-spy ::env :simulators/change {::some ::details})))

              (testing "responds with the details"
                (is (= [:ok {::some ::details}] result)))))

          (testing "when resetting the messages"
            (spies/reset! reset-messages-spy details-spy publish-spy)
            (let [result (handler {:body {:action :simulators.ws/reset-messages}})]
              (testing "takes the requested action"
                (is (spies/called-with? reset-messages-spy ::simulator)))

              (testing "gets the details"
                (is (spies/called-with? details-spy ::simulator)))

              (testing "publishes the event"
                (is (spies/called-with? publish-spy ::env :simulators.ws/reset-messages {::some ::details})))

              (testing "responds with the details"
                (is (= [:ok {::some ::details}] result)))))

          (testing "when disconnecting all sockets"
            (spies/reset! disconnect-spy details-spy publish-spy)
            (let [result (handler {:body {:action :simulators.ws/disconnect-all}})]
              (testing "takes the requested action"
                (is (spies/called-with? disconnect-spy ::simulator)))

              (testing "gets the details"
                (is (spies/called-with? details-spy ::simulator)))

              (testing "does not publish an event"
                (is (spies/never-called? publish-spy)))

              (testing "responds with the details"
                (is (= [:ok {::some ::details}] result)))))

          (testing "when disconnecting one socket"
            (spies/reset! disconnect-spy details-spy publish-spy)
            (let [socket-id (uuids/random)
                  result (handler {:body {:action :simulators.ws/disconnect :socket-id socket-id}})]
              (testing "takes the requested action"
                (is (spies/called-with? disconnect-spy ::simulator socket-id)))

              (testing "gets the details"
                (is (spies/called-with? details-spy ::simulator)))

              (testing "does not publish an event"
                (is (spies/never-called? publish-spy)))

              (testing "responds with the details"
                (is (= [:ok {::some ::details :socket-id socket-id}] result)))))

          (testing "when an unknown action is patched"
            (spies/reset! publish-spy)
            (handler {:body {:action :unknown}})
            (testing "does not publish an action"
              (spies/never-called? publish-spy)))

          (testing "when the action is a string"
            (spies/reset! reset-spy publish-spy)
            (handler {:body {:action "simulators/reset"}})
            (testing "converts the action to a keyword"
              (is (spies/called-with? reset-spy ::simulator))
              (is (spies/called-with? publish-spy ::env :simulators/reset {::some ::details}))))

          (testing "when the socket-id is a string"
            (spies/reset! disconnect-spy publish-spy)
            (let [socket-id (uuids/random)]
              (handler {:body {:action :simulators.ws/disconnect :socket-id (str socket-id)}})
              (testing "converts the socket-id to a UUID"
                (is (spies/called-with? disconnect-spy ::simulator socket-id))))))))))

(deftest ^:unit send-ws-test
  (testing "(send-ws)"
    (let [send-spy (spies/create)]
      (with-redefs [common/send send-spy]
        (let [handler (routes.sim/send-ws ::simulator)
              socket-id (uuids/random)
              request {:body "some-message"}
              result (handler request)]
          (testing "sends the body"
            (is (spies/called-with? send-spy ::simulator "some-message")))

          (testing "responds with success"
            (is (test.http/success? (respond/with result))))

          (testing "when the body is an InputStream"
            (spies/reset! send-spy)
            (handler (assoc request :body (as-stream "some-stream")))

            (testing "sends the slurped body"
              (is (spies/called-with? send-spy ::simulator "some-stream"))))

          (testing "when sending to a socket-id"
            (spies/reset! send-spy)
            (handler (assoc-in request [:params :socket-id] socket-id))

            (testing "sends to that socket-id"
              (is (spies/called-with? send-spy ::simulator socket-id "some-message")))

            (testing "and when the socket-id is a UUID string"
              (spies/reset! send-spy)
              (handler (assoc-in request [:params :socket-id] (str socket-id)))

              (testing "transforms the socket-id to a UUID"
                (is (spies/called-with? send-spy ::simulator socket-id "some-message"))))))))))

(deftest ^:unit disconnect-ws-test
  (testing "(disconnect-ws)"
    (let [disconnect-spy (spies/create)]
      (with-redefs [common/disconnect disconnect-spy]
        (let [handler (routes.sim/disconnect-ws ::simulator)
              result (handler {})]
          (testing "disconnects the sockets"
            (is (spies/called-with? disconnect-spy ::simulator)))

          (testing "responds with success"
            (is (test.http/success? (respond/with result))))

          (testing "when there is a socket-id"
            (spies/reset! disconnect-spy)
            (handler ::request)))))))

(deftest ^:unit http-routes-test
  (testing "(http-routes)"
    (let [details-spy (spies/constantly {:config {:method ::method :path "/some/path"}
                                                  :id     "some-id"})
          get-sim-spy (spies/constantly ::get-sim)
          delete-sim-spy (spies/constantly ::delete-sim)
          patch-sim-spy (spies/constantly ::patch-sim)
          sim-route-spy (spies/constantly ::sim-route)]
      (with-redefs [common/details details-spy
                    routes.sim/get-sim get-sim-spy
                    routes.sim/delete-sim delete-sim-spy
                    routes.sim/patch-sim patch-sim-spy
                    routes.sim/http-sim-route sim-route-spy]
        (testing "contains a sim route"
          (spies/reset! details-spy sim-route-spy)
          (is (-> (routes.sim/http-routes ::env ::simulator)
                  (find-by-method-and-path :method "/simulators/some/path")
                  (= ::sim-route)))
          (is (spies/called-with? sim-route-spy ::env ::simulator)))

        (testing "when the path is /"
          (spies/respond-with! details-spy (constantly {:config {:method ::method :path "/"}
                                                        :id     "some-id"}))
          (testing "drops the trailing slash on the sim route"
            (is (-> (routes.sim/http-routes ::env ::simulator)
                    (find-by-method-and-path :method "/simulators")
                    (= ::sim-route)))))

        (testing "contains routes to get the simulator"
          (spies/reset! details-spy get-sim-spy)
          (let [sims (routes.sim/http-routes ::env ::simulator)]
            (is (spies/called-with? get-sim-spy ::simulator))
            (is (= ::get-sim
                   (find-by-method-and-path sims :get "/api/simulators/method/some/path")))
            (is (= ::get-sim
                   (find-by-method-and-path sims :get "/api/simulators/some-id")))))

        (testing "when building routes to delete the simulator"
          (spies/reset! details-spy delete-sim-spy)
          (let [sims (routes.sim/http-routes ::env ::simulator)]
            (testing "has the routes"
              (is (= ::delete-sim
                     (find-by-method-and-path sims :delete "/api/simulators/method/some/path")))
              (is (= ::delete-sim
                     (find-by-method-and-path sims :delete "/api/simulators/some-id"))))))

        (testing "contains routes to update the simulator"
          (spies/reset! details-spy patch-sim-spy)
          (let [sims (routes.sim/http-routes ::env ::simulator)]
            (is (spies/called-with? patch-sim-spy ::env ::simulator))
            (is (= ::patch-sim
                   (find-by-method-and-path sims :patch "/api/simulators/method/some/path")))
            (is (= ::patch-sim
                   (find-by-method-and-path sims :patch "/api/simulators/some-id")))))))))

(deftest ^:unit ws-routes-test
  (testing "(ws-routes)"
    (let [details-spy (spies/constantly {:config {:path "/some/path"} :id 123})
          remove-spy (spies/create)
          delete-sim-spy (spies/constantly ::delete-sim)
          ws-sim-spy (spies/constantly ::connect)
          get-sim-spy (spies/constantly ::details)
          send-ws-spy (spies/constantly ::send)
          patch-ws-spy (spies/constantly ::reset)
          disconnect-spy (spies/constantly ::disconnect)]
      (with-redefs [common/details details-spy
                    sims/remove! remove-spy
                    routes.sim/delete-sim delete-sim-spy
                    routes.sim/ws-sim-route ws-sim-spy
                    routes.sim/get-sim get-sim-spy
                    routes.sim/send-ws send-ws-spy
                    routes.sim/patch-ws patch-ws-spy
                    routes.sim/disconnect-ws disconnect-spy]
        (let [simulators (set (routes.sim/ws-routes ::env ::simulator))]
          (testing "gets the simulator's details"
            (is (spies/called-with? details-spy ::simulator)))

          (testing "when creating a handler to delete"
            (testing "calls delete-sim"
              (is (spies/called-with? delete-sim-spy ::env ::simulator (spies/matcher fn?))))

            (testing "has a function for removing the simulator"
              (let [[_ _ f] (first (spies/calls delete-sim-spy))]
                (f ::simulator)
                (is (spies/called-with? remove-spy ::env ::simulator)))))

          (testing "creates a handler to connect"
            (is (spies/called-with? ws-sim-spy ::simulator)))

          (testing "creates a handler to get details"
            (is (spies/called-with? get-sim-spy ::simulator)))

          (testing "creates a handler to send messages"
            (is (spies/called-with? send-ws-spy ::simulator)))

          (testing "creates a handler to reset the simulator"
            (is (spies/called-with? patch-ws-spy ::env ::simulator)))

          (testing "creates a handler to disconnect sockets"
            (is (spies/called-with? disconnect-spy ::simulator)))

          (testing "returns the simulators"
            (doseq [sim [[:get "/simulators/some/path" ::connect]
                         [:get "/api/simulators/ws/some/path" ::details]
                         [:get "/api/simulators/123" ::details]
                         [:delete "/api/simulators/ws/some/path" ::delete-sim]
                         [:delete "/api/simulators/123" ::delete-sim]
                         [:post "/api/simulators/ws/some/path" ::send]
                         [:post "/api/simulators/123" ::send]
                         [:post "/api/simulators/ws/some/path/:socket-id" ::send]
                         [:post "/api/simulators/123/:socket-id" ::send]
                         [:delete "/api/simulators/ws/some/path" ::disconnect]
                         [:delete "/api/simulators/123" ::disconnect]
                         [:patch "/api/simulators/ws/some/path" ::reset]
                         [:patch "/api/simulators/123" ::reset]]]
              (is (contains? simulators sim)))))

        (testing "when path is /"
          (spies/reset! details-spy remove-spy delete-sim-spy ws-sim-spy
                        get-sim-spy send-ws-spy patch-ws-spy disconnect-spy)
          (spies/respond-with! details-spy (constantly {:config {:path "/"} :id 123}))
          (let [simulators (set (routes.sim/ws-routes ::env ::simulator))]
            (testing "returns simulators with the correct paths"
              (doseq [sim [[:get "/simulators" ::connect]
                           [:get "/api/simulators/ws" ::details]
                           [:get "/api/simulators/123" ::details]
                           [:delete "/api/simulators/ws" ::delete-sim]
                           [:delete "/api/simulators/123" ::delete-sim]
                           [:post "/api/simulators/ws" ::send]
                           [:post "/api/simulators/123" ::send]
                           [:post "/api/simulators/ws/:socket-id" ::send]
                           [:post "/api/simulators/123/:socket-id" ::send]
                           [:delete "/api/simulators/ws" ::disconnect]
                           [:delete "/api/simulators/123" ::disconnect]
                           [:patch "/api/simulators/ws" ::reset]
                           [:patch "/api/simulators/123" ::reset]]]
                (is (contains? simulators sim))))))))))

(deftest ^:unit http-sim->routes-test
  (testing "(http-sim->routes)"
    (let [routes-spy (spies/constantly [[::route ::1] [::route ::2]])
          make-route-spy (spies/create (comp second vector))]
      (with-redefs [routes.sim/http-routes routes-spy
                    c/make-route make-route-spy]
        (testing "make routes"
          (let [routes (routes.sim/http-sim->routes ::env ::simulator)]
            (is (= [::1 ::2] routes))
            (is (spies/called-with? routes-spy ::env ::simulator))
            (is (spies/called-with? make-route-spy ::route ::1))
            (is (spies/called-with? make-route-spy ::route ::2))))))))

(deftest ^:unit ws-sim->routes-test
  (testing "(ws-sim->routes)"
    (let [routes-spy (spies/constantly [[::route ::1] [::route ::2]])
          make-route-spy (spies/create (comp second vector))]
      (with-redefs [routes.sim/ws-routes routes-spy
                    c/make-route make-route-spy]
        (testing "make routes"
          (let [routes (routes.sim/ws-sim->routes ::env ::simulator)]
            (is (= [::1 ::2] routes))
            (is (spies/called-with? routes-spy ::env ::simulator))
            (is (spies/called-with? make-route-spy ::route ::1))
            (is (spies/called-with? make-route-spy ::route ::2))))))))

(deftest ^:unit file-sim->routes-test
  (testing "(file-sim->routes)"
    (let [routes-spy (spies/constantly [[::route ::1] [::route ::2]])
          make-route-spy (spies/create (comp second vector))]
      (with-redefs [routes.sim/http-routes routes-spy
                    c/make-route make-route-spy]
        (testing "make routes"
          (let [routes (routes.sim/file-sim->routes ::env ::simulator)]
            (is (= [::1 ::2] routes))
            (is (spies/called-with? routes-spy ::env ::simulator))
            (is (spies/called-with? make-route-spy ::route ::1))
            (is (spies/called-with? make-route-spy ::route ::2))))))))
