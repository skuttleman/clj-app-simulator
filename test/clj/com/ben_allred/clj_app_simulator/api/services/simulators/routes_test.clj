(ns com.ben-allred.clj-app-simulator.api.services.simulators.routes-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.api.services.simulators.routes :as routes.sim]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
            [compojure.core :as c]
            [com.ben-allred.clj-app-simulator.api.services.activity :as activity]
            [com.ben-allred.clj-app-simulator.services.http :as http]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(defn ^:private find-by-method [routes method]
  (->> routes
       (filter (comp #{method} first))
       (map last)
       (first)))

(deftest ^:unit http-sim->routes-test
  (testing "(http-sim->routes)"
    (let [config {:method :http-method :path "/simulator/path" :requests ::requests}
          make-route-spy (spies/create vector)
          change-spy (spies/create)
          config-spy (spies/create (constantly config))
          delete-spy (spies/create)
          details-spy (spies/create (constantly (assoc config :details ::details)))
          pub-spy (spies/create)
          receive-spy (spies/create (constantly ::response))
          requests-spy (spies/create (constantly [::request-1 ::request-2]))
          reset-spy (spies/create)
          reset-requests-spy (spies/create)
          reset-response-spy (spies/create)
          simulator (reify
                      common/ISimulator
                      (config [this]
                        (config-spy this))
                      (receive [this request]
                        (receive-spy this request))
                      (requests [this]
                        (requests-spy this))
                      (details [this]
                        (details-spy this))
                      (reset [this]
                        (reset-spy this))
                      common/IHTTPSimulator
                      (reset-requests [this]
                        (reset-requests-spy this))
                      (reset-response [this]
                        (reset-response-spy this))
                      (change [this config]
                        (change-spy this config)))]
      (with-redefs [c/make-route make-route-spy
                    activity/publish pub-spy]
        (let [routes (vec (routes.sim/http-sim->routes simulator delete-spy))]
          (testing "having a simulator route"
            (spies/reset! config-spy pub-spy receive-spy requests-spy)
            (let [handler (find-by-method routes :http-method)
                  result (handler ::request)]
              (Thread/sleep 10)
              (testing "receives the request"
                (is (spies/called-with? receive-spy simulator ::request)))
              (testing "publishes an event"
                (is (spies/called-with? config-spy simulator))
                (is (spies/called-with? requests-spy simulator))
                (is (spies/called-with? pub-spy :simulators/receive {:simulator (dissoc config :requests) :request ::request-2})))
              (testing "returns the response"
                (is (= ::response result)))))
          (testing "having a details route"
            (spies/reset! details-spy)
            (let [handler (find-by-method routes :get)
                  result (handler ::request)]
              (testing "returns a success response"
                (is (spies/called-with? details-spy simulator))
                (is (http/success? result))
                (is (= (assoc config :details ::details) (get-in result [:body :simulator]))))))
          (testing "having a delete route"
            (spies/reset! delete-spy pub-spy)
            (let [handler (find-by-method routes :delete)
                  result (handler ::request)]
              (testing "publishes an event"
                (is (spies/called-with? pub-spy :simulators/delete (select-keys config #{:path :method}))))
              (testing "deletes the simulator"
                (is (spies/called-with? delete-spy :http-method "/simulator/path")))
              (testing "returns a success response"
                (is (http/success? result)))))
          (testing "having an update route"
            (let [handler (find-by-method routes :patch)]
              (testing "when the update succeeds"
                (testing "resets the simulator"
                  (spies/reset! reset-spy pub-spy)
                  (let [result (handler {:body {:action "simulator/reset"}})]
                    (is (spies/called-with? reset-spy simulator))
                    (is (spies/called-with? pub-spy :simulator/reset config))
                    (is (http/success? result))))
                (testing "resets the simulator's requests"
                  (spies/reset! reset-requests-spy pub-spy)
                  (let [result (handler {:body {:action :http/reset-requests}})]
                    (is (spies/called-with? reset-requests-spy simulator))
                    (is (spies/called-with? pub-spy :http/reset-requests (select-keys config #{:method :path})))
                    (is (http/success? result))))
                (testing "resets the simulator's response"
                  (spies/reset! reset-response-spy pub-spy)
                  (let [result (handler {:body {:action :http/reset-response}})]
                    (is (spies/called-with? reset-response-spy simulator))
                    (is (spies/called-with? pub-spy :http/reset-response config))
                    (is (http/success? result))))
                (testing "changes the simulator"
                  (spies/reset! change-spy pub-spy)
                  (let [result (handler {:body {:action :http/change
                                                :config {::more ::config}}})]
                    (is (spies/called-with? change-spy simulator {::more ::config}))
                    (is (spies/called-with? pub-spy :http/change config))
                    (is (http/success? result)))))
              (testing "when the update fails"
                (spies/reset! change-spy)
                (spies/respond-with! change-spy (fn [& _] (throw (ex-info "just 'cause" {:problems ::problems}))))
                (testing "returns an error response"
                  (let [result (handler {:body {:action :http/change
                                                :config ::config}})]
                    (is (= ::problems (:body result)))
                    (is (http/client-error? result)))))))
          (testing "makes routes"
            (is (spies/called-with? make-route-spy :http-method "/simulators/simulator/path" (spies/matcher fn?)))
            (is (spies/called-with? make-route-spy :get "/api/simulators/http-method/simulator/path" (spies/matcher fn?)))
            (is (spies/called-with? make-route-spy :delete "/api/simulators/http-method/simulator/path" (spies/matcher fn?)))
            (is (spies/called-with? make-route-spy :patch "/api/simulators/http-method/simulator/path" (spies/matcher fn?)))))))))
