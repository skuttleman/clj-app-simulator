(ns com.ben-allred.app-simulator.api.services.simulators.routes-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.app-simulator.api.services.activity :as activity]
    [com.ben-allred.app-simulator.api.services.simulators.common :as common]
    [com.ben-allred.app-simulator.api.services.simulators.routes :as routes.sim]
    [com.ben-allred.app-simulator.api.services.simulators.simulators :as sims]
    [com.ben-allred.app-simulator.api.utils.respond :as respond]
    [com.ben-allred.app-simulator.api.utils.specs :as specs]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [com.ben-allred.app-simulator.utils.uuids :as uuids]
    [compojure.core :as c]
    [integration.utils.http :as test.http]
    [test.utils.spies :as spies])
  (:import
    (clojure.lang ExceptionInfo)
    (java.io ByteArrayInputStream)))

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
    (let [details {:id ::id :config ::config :other ::things}
          publish-spy (spies/create)
          details-spy (spies/constantly details)
          request-spy (spies/constantly [::request-1 ::request-2])]
      (with-redefs [activity/publish publish-spy
                    common/details details-spy
                    common/received request-spy]
        (testing "publishes an event"
          (routes.sim/receive ::env ::simulator)
          (is (spies/called-with? details-spy ::simulator))
          (is (spies/called-with? request-spy ::simulator))
          (is (spies/called-with? publish-spy
                                  ::env
                                  :simulators/receive
                                  {:simulator details
                                   :request   ::request-2})))))))

(deftest ^:unit http-sim-route-test
  (testing "(http-sim-route)"
    (let [receive-spy (spies/constantly ::response)
          publish-spy (spies/create)
          receive-publish-spy (spies/create)]
      (with-redefs [common/receive! receive-spy
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
            (is (spies/called-with? receive-publish-spy ::env ::simulator)))

          (testing "returns the response"
            (is (= ::response (sim {::a ::request})))))))))

(deftest ^:unit ws-sim-route-test
  (testing "(ws-sim-route)"
    (let [connect-spy (spies/constantly ::socket-upgrade)]
      (with-redefs [common/connect! connect-spy]
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
            (is (= [:http.status/ok {:simulator ::details}] result))))))))

(deftest ^:unit delete-sim-test
  (testing "(delete-sim)"
    (let [details {:id       ::id
                   :config   ::config
                   :details  ::details
                   :requests ::requests}
          details-spy (spies/constantly details)
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
                                    {:simulator details}))

            (is (spies/called-with? identifier-spy ::simulator))
            (is (spies/called-with? delete-spy ::identifier))
            (is (= result [:http.status/no-content]))))))))

(deftest ^:unit patch-test
  (testing "(patch)"
    (let [reset-spy (spies/create)
          reset-messages-spy (spies/create)
          change-spy (spies/create)
          disconnect-spy (spies/create)
          details-spy (spies/constantly {::some ::details})
          publish-spy (spies/create)
          conform-spy (spies/create (fn [_ body] body))]
      (with-redefs [common/reset! (fn [& args]
                                    (if (= 1 (count args))
                                      (apply reset-spy args)
                                      (apply change-spy args)))
                    common/partially-reset! reset-messages-spy
                    common/disconnect! disconnect-spy
                    common/details details-spy
                    activity/publish publish-spy
                    specs/conform conform-spy]
        (let [handler (routes.sim/patch ::env ::simulator :sim-type)]
          (testing "when resetting the simulator"
            (spies/reset! reset-spy details-spy publish-spy)
            (let [result (handler {:body {:action :simulators/reset}})]
              (testing "conforms the body"
                (is (spies/called-with? conform-spy :simulator.sim-type/patch {:action :simulators/reset})))

              (testing "takes the requested action"
                (is (spies/called-with? reset-spy ::simulator)))

              (testing "gets the details"
                (is (spies/called-with? details-spy ::simulator)))

              (testing "publishes the event"
                (is (spies/called-with? publish-spy ::env :simulators/reset {:simulator {::some ::details}})))

              (testing "responds with the details"
                (is (= [:http.status/ok {:simulator {::some ::details}}] result)))))

          (testing "when changing the simulator"
            (spies/reset! change-spy details-spy publish-spy)
            (let [result (handler {:body {:action :simulators/change :config ::config}})]
              (testing "takes the requested action"
                (is (spies/called-with? change-spy ::simulator ::config)))

              (testing "gets the details"
                (is (spies/called-with? details-spy ::simulator)))

              (testing "publishes the event"
                (is (spies/called-with? publish-spy ::env :simulators/change {:simulator {::some ::details}})))

              (testing "responds with the details"
                (is (= [:http.status/ok {:simulator {::some ::details}}] result)))))

          (testing "when resetting the messages"
            (spies/reset! reset-messages-spy details-spy publish-spy)
            (let [result (handler {:body {:action :simulators/reset :type :ws/requests}})]
              (testing "takes the requested action"
                (is (spies/called-with? reset-messages-spy ::simulator :ws/requests)))

              (testing "gets the details"
                (is (spies/called-with? details-spy ::simulator)))

              (testing "publishes the event"
                (is (spies/called-with? publish-spy ::env :simulators/reset {:simulator {::some ::details}})))

              (testing "responds with the details"
                (is (= [:http.status/ok {:simulator {::some ::details}}] result)))))

          (testing "when disconnecting all sockets"
            (spies/reset! disconnect-spy details-spy publish-spy)
            (let [result (handler {:body {:action :simulators.ws/disconnect}})]
              (testing "takes the requested action"
                (is (spies/called-with? disconnect-spy ::simulator)))

              (testing "gets the details"
                (is (spies/called-with? details-spy ::simulator)))

              (testing "publishes an event"
                (is (spies/called-with? publish-spy ::env :simulators.ws/disconnect {:simulator {::some ::details}})))

              (testing "responds with the details"
                (is (= [:http.status/ok {:simulator {::some ::details}}] result)))))

          (testing "when disconnecting one socket"
            (spies/reset! disconnect-spy details-spy publish-spy)
            (let [socket-id (uuids/random)
                  result (handler {:body {:action :simulators.ws/disconnect :socket-id socket-id}})]
              (testing "takes the requested action"
                (is (spies/called-with? disconnect-spy ::simulator socket-id)))

              (testing "gets the details"
                (is (spies/called-with? details-spy ::simulator)))

              (testing "publishes an event"
                (is (spies/called-with? publish-spy ::env :simulators.ws/disconnect {:simulator {::some ::details} :socket-id socket-id})))

              (testing "responds with the details"
                (is (= [:http.status/ok {:simulator {::some ::details}}] result)))))

          (testing "when the body does not conform to the spec"
            (spies/reset! publish-spy)
            (spies/respond-with! conform-spy (constantly nil))
            (testing "throws an exception"
              (is (thrown? ExceptionInfo (handler {:body {:action :unknown}}))))

            (testing "does not publish an action"
              (spies/never-called? publish-spy))))))))

(deftest ^:unit send-ws-test
  (testing "(send-ws)"
    (let [send-spy (spies/create)]
      (with-redefs [common/send! send-spy]
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
      (with-redefs [common/disconnect! disconnect-spy]
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
          patch-spy (spies/constantly ::patch-sim)
          sim-route-spy (spies/constantly ::sim-route)]
      (with-redefs [common/details details-spy
                    routes.sim/get-sim get-sim-spy
                    routes.sim/delete-sim delete-sim-spy
                    routes.sim/patch patch-spy
                    routes.sim/http-sim-route sim-route-spy]
        (testing "contains a sim route"
          (spies/reset! details-spy sim-route-spy)
          (is (-> (routes.sim/http-routes ::type ::env ::simulator)
                  (find-by-method-and-path :method "/simulators/some/path")
                  (= ::sim-route)))
          (is (spies/called-with? sim-route-spy ::env ::simulator)))

        (testing "when the path is /"
          (spies/respond-with! details-spy (constantly {:config {:method ::method :path "/"}
                                                        :id     "some-id"}))
          (testing "drops the trailing slash on the sim route"
            (is (-> (routes.sim/http-routes ::type ::env ::simulator)
                    (find-by-method-and-path :method "/simulators")
                    (= ::sim-route)))))

        (testing "contains routes to get the simulator"
          (spies/reset! details-spy get-sim-spy)
          (let [sims (routes.sim/http-routes ::type ::env ::simulator)]
            (is (spies/called-with? get-sim-spy ::simulator))
            (is (= ::get-sim
                   (find-by-method-and-path sims :get "/api/simulators/some-id")))))

        (testing "when building routes to delete the simulator"
          (spies/reset! details-spy delete-sim-spy)
          (let [sims (routes.sim/http-routes ::type ::env ::simulator)]
            (testing "has the routes"
              (is (= ::delete-sim
                     (find-by-method-and-path sims :delete "/api/simulators/some-id"))))))

        (testing "contains routes to update the simulator"
          (spies/reset! details-spy patch-spy)
          (let [sims (routes.sim/http-routes ::type ::env ::simulator)]
            (is (spies/called-with? patch-spy ::env ::simulator ::type))
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
          patch-spy (spies/constantly ::reset)
          disconnect-spy (spies/constantly ::disconnect)]
      (with-redefs [common/details details-spy
                    sims/remove! remove-spy
                    routes.sim/delete-sim delete-sim-spy
                    routes.sim/ws-sim-route ws-sim-spy
                    routes.sim/get-sim get-sim-spy
                    routes.sim/send-ws send-ws-spy
                    routes.sim/patch patch-spy
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
            (is (spies/called-with? patch-spy ::env ::simulator :ws)))

          (testing "creates a handler to disconnect sockets"
            (is (spies/called-with? disconnect-spy ::simulator)))

          (testing "returns the simulators"
            (are [method path handler] (contains? simulators [method path handler])
              :get "/simulators/some/path" ::connect
              :get "/api/simulators/123" ::details
              :delete "/api/simulators/123" ::delete-sim
              :post "/api/simulators/123" ::send
              :post "/api/simulators/123/sockets/:socket-id" ::send
              :delete "/api/simulators/123" ::disconnect
              :patch "/api/simulators/123" ::reset)))

        (testing "when path is /"
          (spies/reset! details-spy remove-spy delete-sim-spy ws-sim-spy
                        get-sim-spy send-ws-spy patch-spy disconnect-spy)
          (spies/respond-with! details-spy (constantly {:config {:path "/"} :id 123}))
          (let [simulators (set (routes.sim/ws-routes ::env ::simulator))]
            (testing "returns simulators with the correct paths"
              (are [method path handler] (contains? simulators [method path handler])
                :get "/simulators" ::connect
                :get "/api/simulators/123" ::details
                :delete "/api/simulators/123" ::delete-sim
                :post "/api/simulators/123" ::send
                :post "/api/simulators/123/sockets/:socket-id" ::send
                :delete "/api/simulators/123" ::disconnect
                :patch "/api/simulators/123" ::reset))))))))

(deftest ^:unit http-sim->routes-test
  (testing "(http-sim->routes)"
    (let [routes-spy (spies/constantly [[::route ::1] [::route ::2]])
          make-route-spy (spies/create (comp second vector))]
      (with-redefs [routes.sim/http-routes routes-spy
                    c/make-route make-route-spy]
        (testing "makes routes"
          (let [routes (routes.sim/http-sim->routes ::env ::simulator)]
            (is (= [::1 ::2] routes))
            (is (spies/called-with? routes-spy :http ::env ::simulator))
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
            (is (spies/called-with? routes-spy :file ::env ::simulator))
            (is (spies/called-with? make-route-spy ::route ::1))
            (is (spies/called-with? make-route-spy ::route ::2))))))))
