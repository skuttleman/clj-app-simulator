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
    (let [details {:id ::id :config ::config :other ::things}]
      (with-redefs [activity/publish (spies/create)
                    common/details (spies/constantly details)
                    common/received (spies/constantly [::request-1 ::request-2])]
        (testing "publishes an event"
          (routes.sim/receive ::env ::simulator)
          (is (spies/called-with? common/details ::simulator))
          (is (spies/called-with? common/received ::simulator))
          (is (spies/called-with? activity/publish
                                  ::env
                                  :simulators/receive
                                  {:simulator details
                                   :request   ::request-2})))))))

(deftest ^:unit http-sim-route-test
  (testing "(http-sim-route)"
    (with-redefs [common/receive! (spies/constantly ::response)
                  activity/publish (constantly nil)
                  routes.sim/receive (spies/create)]
      (let [sim (routes.sim/http-sim-route ::env ::simulator)]
        (testing "receives the request"
          (spies/reset! common/receive!)
          (let [request {:body ::body :data ::data}]
            (sim request)
            (is (spies/called-with? common/receive! ::simulator request))))

        (testing "when the request :body is an InputStream"
          (testing "slurps and trims the :body"
            (spies/reset! common/receive!)
            (let [request {:body (as-stream "   a body\t")}]
              (sim request)
              (is (spies/called-with? common/receive! ::simulator {:body "a body"}))))

          (testing "and when the trimmed :body is empty"
            (testing "stores the body as nil"
              (spies/reset! common/receive!)
              (let [request {:body (as-stream "\n  ")}]
                (sim request)
                (is (nil? (:body (last (first (spies/calls common/receive!))))))))))

        (testing "publishes an event"
          (spies/reset! routes.sim/receive)
          (sim {::a ::request})
          (is (spies/called-with? routes.sim/receive ::env ::simulator)))

        (testing "returns the response"
          (is (= ::response (sim {::a ::request}))))))))

(deftest ^:unit ws-sim-route-test
  (testing "(ws-sim-route)"
    (with-redefs [common/connect! (spies/constantly ::socket-upgrade)]
      (let [result ((routes.sim/ws-sim-route ::simulator) ::request)]
        (testing "connects the socket request"
          (is (spies/called-with? common/connect! ::simulator ::request)))

        (testing "returns the socket upgrade"
          (is (= ::socket-upgrade result)))))))

(deftest ^:unit get-sim-test
  (testing "(get-sim)"
    (with-redefs [common/details (spies/constantly ::details)]
      (let [handler (routes.sim/get-sim ::simulator)
            result (handler ::request)]
        (testing "responds with the simulator's details"
          (is (spies/called-with? common/details ::simulator))
          (is (= [:http.status/ok {:simulator ::details}] result)))))))

(deftest ^:unit delete-sim-test
  (testing "(delete-sim)"
    (let [details {:id       ::id
                   :config   ::config
                   :details  ::details
                   :requests ::requests}
          delete-spy (spies/create)]
      (with-redefs [common/details (spies/constantly details)
                    common/identifier (spies/constantly ::identifier)
                    activity/publish (spies/create)]
        (testing "deletes the simulator"
          (let [handler (routes.sim/delete-sim ::env ::simulator delete-spy)
                result (handler ::request)]
            (is (spies/called-with? activity/publish
                                    ::env
                                    :simulators/delete
                                    {:simulator details}))

            (is (spies/called-with? common/identifier ::simulator))
            (is (spies/called-with? delete-spy ::identifier))
            (is (= result [:http.status/no-content]))))))))

(deftest ^:unit patch-test
  (testing "(patch)"
    (let [reset-spy (spies/create)
          change-spy (spies/create)]
      (with-redefs [common/reset! (fn [& args]
                                    (if (= 1 (count args))
                                      (apply reset-spy args)
                                      (apply change-spy args)))
                    common/partially-reset! (spies/create)
                    common/disconnect! (spies/create)
                    common/details (spies/constantly {::some ::details})
                    activity/publish (spies/create)
                    specs/conform (spies/create (fn [_ body] body))]
        (let [handler (routes.sim/patch ::env ::simulator :sim-type)]
          (testing "when resetting the simulator"
            (spies/reset! reset-spy common/details activity/publish)
            (let [result (handler {:body {:action :simulators/reset}})]
              (testing "conforms the body"
                (is (spies/called-with? specs/conform :simulator.sim-type/patch {:action :simulators/reset})))

              (testing "takes the requested action"
                (is (spies/called-with? reset-spy ::simulator)))

              (testing "gets the details"
                (is (spies/called-with? common/details ::simulator)))

              (testing "publishes the event"
                (is (spies/called-with? activity/publish ::env :simulators/reset {:simulator {::some ::details}})))

              (testing "responds with the details"
                (is (= [:http.status/ok {:simulator {::some ::details}}] result)))))

          (testing "when changing the simulator"
            (spies/reset! change-spy common/details activity/publish)
            (let [result (handler {:body {:action :simulators/change :config ::config}})]
              (testing "takes the requested action"
                (is (spies/called-with? change-spy ::simulator ::config)))

              (testing "gets the details"
                (is (spies/called-with? common/details ::simulator)))

              (testing "publishes the event"
                (is (spies/called-with? activity/publish ::env :simulators/change {:simulator {::some ::details}})))

              (testing "responds with the details"
                (is (= [:http.status/ok {:simulator {::some ::details}}] result)))))

          (testing "when resetting the messages"
            (spies/reset! common/partially-reset! common/details activity/publish)
            (let [result (handler {:body {:action :simulators/reset :type :ws/requests}})]
              (testing "takes the requested action"
                (is (spies/called-with? common/partially-reset! ::simulator :ws/requests)))

              (testing "gets the details"
                (is (spies/called-with? common/details ::simulator)))

              (testing "publishes the event"
                (is (spies/called-with? activity/publish ::env :simulators/reset {:simulator {::some ::details}})))

              (testing "responds with the details"
                (is (= [:http.status/ok {:simulator {::some ::details}}] result)))))

          (testing "when disconnecting all sockets"
            (spies/reset! common/disconnect! common/details activity/publish)
            (let [result (handler {:body {:action :simulators.ws/disconnect}})]
              (testing "takes the requested action"
                (is (spies/called-with? common/disconnect! ::simulator)))

              (testing "gets the details"
                (is (spies/called-with? common/details ::simulator)))

              (testing "publishes an event"
                (is (spies/called-with? activity/publish ::env :simulators.ws/disconnect {:simulator {::some ::details}})))

              (testing "responds with the details"
                (is (= [:http.status/ok {:simulator {::some ::details}}] result)))))

          (testing "when disconnecting one socket"
            (spies/reset! common/disconnect! common/details activity/publish)
            (let [socket-id (uuids/random)
                  result (handler {:body {:action :simulators.ws/disconnect :socket-id socket-id}})]
              (testing "takes the requested action"
                (is (spies/called-with? common/disconnect! ::simulator socket-id)))

              (testing "gets the details"
                (is (spies/called-with? common/details ::simulator)))

              (testing "publishes an event"
                (is (spies/called-with? activity/publish ::env :simulators.ws/disconnect {:simulator {::some ::details} :socket-id socket-id})))

              (testing "responds with the details"
                (is (= [:http.status/ok {:simulator {::some ::details}}] result)))))

          (testing "when the body does not conform to the spec"
            (spies/reset! activity/publish)
            (spies/respond-with! specs/conform (constantly nil))
            (testing "throws an exception"
              (is (thrown? ExceptionInfo (handler {:body {:action :unknown}}))))

            (testing "does not publish an action"
              (spies/never-called? activity/publish))))))))

(deftest ^:unit send-ws-test
  (testing "(send-ws)"
    (with-redefs [common/send! (spies/create)]
      (let [handler (routes.sim/send-ws ::simulator)
            socket-id (uuids/random)
            request {:body "some-message"}
            result (handler request)]
        (testing "sends the body"
          (is (spies/called-with? common/send! ::simulator "some-message")))

        (testing "responds with success"
          (is (test.http/success? (respond/with result))))

        (testing "when the body is an InputStream"
          (spies/reset! common/send!)
          (handler (assoc request :body (as-stream "some-stream")))

          (testing "sends the slurped body"
            (is (spies/called-with? common/send! ::simulator "some-stream"))))

        (testing "when sending to a socket-id"
          (spies/reset! common/send!)
          (handler (assoc-in request [:params :socket-id] socket-id))

          (testing "sends to that socket-id"
            (is (spies/called-with? common/send! ::simulator socket-id "some-message")))

          (testing "and when the socket-id is a UUID string"
            (spies/reset! common/send!)
            (handler (assoc-in request [:params :socket-id] (str socket-id)))

            (testing "transforms the socket-id to a UUID"
              (is (spies/called-with? common/send! ::simulator socket-id "some-message")))))))))

(deftest ^:unit disconnect-ws-test
  (testing "(disconnect-ws)"
    (with-redefs [common/disconnect! (spies/create)]
      (let [handler (routes.sim/disconnect-ws ::simulator)
            result (handler {})]
        (testing "disconnects the sockets"
          (is (spies/called-with? common/disconnect! ::simulator)))

        (testing "responds with success"
          (is (test.http/success? (respond/with result))))

        (testing "when there is a socket-id"
          (spies/reset! common/disconnect!)
          (handler ::request))))))

(deftest ^:unit http-routes-test
  (testing "(http-routes)"
    (with-redefs [common/details (spies/constantly {:config {:method ::method :path "/some/path"}
                                                    :id     "some-id"})
                  routes.sim/get-sim (spies/constantly ::get-sim)
                  routes.sim/delete-sim (spies/constantly ::delete-sim)
                  routes.sim/patch (spies/constantly ::patch-sim)
                  routes.sim/http-sim-route (spies/constantly ::sim-route)]
      (testing "contains a sim route"
        (spies/reset! common/details routes.sim/http-sim-route)
        (is (-> (routes.sim/http-routes ::type ::env ::simulator)
                (find-by-method-and-path :method "/simulators/some/path")
                (= ::sim-route)))
        (is (spies/called-with? routes.sim/http-sim-route ::env ::simulator)))

      (testing "when the path is /"
        (spies/respond-with! common/details (constantly {:config {:method ::method :path "/"}
                                                         :id     "some-id"}))
        (testing "drops the trailing slash on the sim route"
          (is (-> (routes.sim/http-routes ::type ::env ::simulator)
                  (find-by-method-and-path :method "/simulators")
                  (= ::sim-route)))))

      (testing "contains routes to get the simulator"
        (spies/reset! common/details routes.sim/get-sim)
        (let [sims (routes.sim/http-routes ::type ::env ::simulator)]
          (is (spies/called-with? routes.sim/get-sim ::simulator))
          (is (= ::get-sim
                 (find-by-method-and-path sims :get "/api/simulators/some-id")))))

      (testing "when building routes to delete the simulator"
        (spies/reset! common/details routes.sim/delete-sim)
        (let [sims (routes.sim/http-routes ::type ::env ::simulator)]
          (testing "has the routes"
            (is (= ::delete-sim
                   (find-by-method-and-path sims :delete "/api/simulators/some-id"))))))

      (testing "contains routes to update the simulator"
        (spies/reset! common/details routes.sim/patch)
        (let [sims (routes.sim/http-routes ::type ::env ::simulator)]
          (is (spies/called-with? routes.sim/patch ::env ::simulator ::type))
          (is (= ::patch-sim
                 (find-by-method-and-path sims :patch "/api/simulators/some-id"))))))))

(deftest ^:unit ws-routes-test
  (testing "(ws-routes)"
    (with-redefs [common/details (spies/constantly {:config {:path "/some/path"} :id 123})
                  sims/remove! (spies/create)
                  routes.sim/delete-sim (spies/constantly ::delete-sim)
                  routes.sim/ws-sim-route (spies/constantly ::connect)
                  routes.sim/get-sim (spies/constantly ::details)
                  routes.sim/send-ws (spies/constantly ::send)
                  routes.sim/patch (spies/constantly ::reset)
                  routes.sim/disconnect-ws (spies/constantly ::disconnect)]
      (let [simulators (set (routes.sim/ws-routes ::env ::simulator))]
        (testing "gets the simulator's details"
          (is (spies/called-with? common/details ::simulator)))

        (testing "when creating a handler to delete"
          (testing "calls delete-sim"
            (is (spies/called-with? routes.sim/delete-sim ::env ::simulator (spies/matcher fn?))))

          (testing "has a function for removing the simulator"
            (let [[_ _ f] (first (spies/calls routes.sim/delete-sim))]
              (f ::simulator)
              (is (spies/called-with? sims/remove! ::env ::simulator)))))

        (testing "creates a handler to connect"
          (is (spies/called-with? routes.sim/ws-sim-route ::simulator)))

        (testing "creates a handler to get details"
          (is (spies/called-with? routes.sim/get-sim ::simulator)))

        (testing "creates a handler to send messages"
          (is (spies/called-with? routes.sim/send-ws ::simulator)))

        (testing "creates a handler to reset the simulator"
          (is (spies/called-with? routes.sim/patch ::env ::simulator :ws)))

        (testing "creates a handler to disconnect sockets"
          (is (spies/called-with? routes.sim/disconnect-ws ::simulator)))

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
        (spies/reset! common/details sims/remove! routes.sim/delete-sim routes.sim/ws-sim-route
                      routes.sim/get-sim routes.sim/send-ws routes.sim/patch routes.sim/disconnect-ws)
        (spies/respond-with! common/details (constantly {:config {:path "/"} :id 123}))
        (let [simulators (set (routes.sim/ws-routes ::env ::simulator))]
          (testing "returns simulators with the correct paths"
            (are [method path handler] (contains? simulators [method path handler])
              :get "/simulators" ::connect
              :get "/api/simulators/123" ::details
              :delete "/api/simulators/123" ::delete-sim
              :post "/api/simulators/123" ::send
              :post "/api/simulators/123/sockets/:socket-id" ::send
              :delete "/api/simulators/123" ::disconnect
              :patch "/api/simulators/123" ::reset)))))))

(deftest ^:unit http-sim->routes-test
  (testing "(http-sim->routes)"
    (with-redefs [routes.sim/http-routes (spies/constantly [[::route ::1] [::route ::2]])
                  c/make-route (spies/create (comp second vector))]
      (testing "makes routes"
        (let [routes (routes.sim/http-sim->routes ::env ::simulator)]
          (is (= [::1 ::2] routes))
          (is (spies/called-with? routes.sim/http-routes :http ::env ::simulator))
          (is (spies/called-with? c/make-route ::route ::1))
          (is (spies/called-with? c/make-route ::route ::2)))))))

(deftest ^:unit ws-sim->routes-test
  (testing "(ws-sim->routes)"
    (with-redefs [routes.sim/ws-routes (spies/constantly [[::route ::1] [::route ::2]])
                  c/make-route (spies/create (comp second vector))]
      (testing "make routes"
        (let [routes (routes.sim/ws-sim->routes ::env ::simulator)]
          (is (= [::1 ::2] routes))
          (is (spies/called-with? routes.sim/ws-routes ::env ::simulator))
          (is (spies/called-with? c/make-route ::route ::1))
          (is (spies/called-with? c/make-route ::route ::2)))))))

(deftest ^:unit file-sim->routes-test
  (testing "(file-sim->routes)"
    (with-redefs [routes.sim/http-routes (spies/constantly [[::route ::1] [::route ::2]])
                  c/make-route (spies/create (comp second vector))]
      (testing "make routes"
        (let [routes (routes.sim/file-sim->routes ::env ::simulator)]
          (is (= [::1 ::2] routes))
          (is (spies/called-with? routes.sim/http-routes :file ::env ::simulator))
          (is (spies/called-with? c/make-route ::route ::1))
          (is (spies/called-with? c/make-route ::route ::2)))))))
