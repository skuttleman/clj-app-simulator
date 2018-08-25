(ns integration.tests.simulators.http
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [integration.utils.fixtures :as fixtures]
            [integration.utils.http :as test.http]
            [integration.utils.ws :as test.ws]
            [clojure.edn :as edn]
            [com.ben-allred.clj-app-simulator.utils.transit :as transit]
            [com.ben-allred.clj-app-simulator.utils.json :as json]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [clojure.core.async :as async]
            [clojure.spec.alpha :as s]
            [com.ben-allred.clj-app-simulator.utils.maps :as maps])
  (:import [java.util Date]))

(use-fixtures :once fixtures/run-server)

(def ^:private content-type->parser
  {"application/edn"     edn/read-string
   "application/transit" transit/parse
   "application/json"    json/parse})

(def ^:private content-type->stringify
  {"application/edn"     pr-str
   "application/transit" transit/stringify
   "application/json"    json/stringify})

(def ^:private uuid-re #"[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}")

(defn ^:private simple [sim]
  (dissoc sim :response))

(def ^:private sim-mapper
  (comp #(:config %) #(s/conform :http/http-simulator %)))

(defn ^:privates sims-match? [sims-1 sims-2]
  (= (->> sims-1
          (map sim-mapper)
          (set))
     (->> sims-2
          (map sim-mapper)
          (set))))

(def ^:private start-configs [{:method   :http/get
                               :path     "/some/:url-param"
                               :delay    1
                               :response {:status  202
                                          :body    (json/stringify {:some :json})
                                          :headers {:content-type    "application/json"
                                                    :x-custom-header "some value"}}}
                              {:method   :http/post
                               :path     "/some/path"
                               :response {:status  201
                                          :body    (pr-str {:some :edn})
                                          :headers {:content-type "application/edn"}}}])

(def ^:private new-sim {:method   "http/delete"
                        :path     "/something/that/cannot/be/deleted"
                        :response {:status  400
                                   :headers {"Some-Header" "some value"}}})

(def ^:private second-sims [{:method   :http/patch
                             :path     "/new/simulator"
                             :response {:status 200}}
                            {:method   :http/get
                             :path     "/another/new/simulator"
                             :response {:status 403}}])

(deftest ^:integration http-simulators-init-test
  (testing "[simulators API]"
    (doseq [content-type ["application/edn" "application/transit" "application/json"]]
      (let [chan (async/chan 64)
            ws (test.ws/connect "/api/simulators/activity"
                                :query-params {:accept content-type}
                                :on-msg (partial async/put! chan)
                                :to-clj (content-type->parser content-type))]
        (testing (str "when saving initial simulators with content-type: " (pr-str content-type))
          (let [response (test.http/post "/api/simulators/init" content-type {:body {:simulators start-configs}})]
            (testing "returns a success response"
              (is (test.http/success? response)))
            (testing "returns ids"
              (let [ids (map :id (:simulators (second response)))]
                (is (= 2 (count ids)))
                (is (re-matches uuid-re (str (first ids))))
                (is (re-matches uuid-re (str (second ids)))))))
          (testing "publishes event on activity feed"
            (let [{:keys [event data]} (async/<!! chan)]
              (is (= :simulators/init (keyword event)))
              (is (sims-match? start-configs data))))
          (testing "and when getting a list of simulators"
            (let [response (test.http/get "/api/simulators" content-type)]
              (is (test.http/success? response))
              (testing "returns a list of simulators"
                (is (sims-match? (map simple start-configs)
                                 (map simple (get-in response [1 :simulators])))))))
          (testing "and when adding a simulator"
            (let [response (test.http/post "/api/simulators" content-type {:body {:simulator new-sim}})]
              (testing "returns a success response"
                (is (test.http/success? response)))
              (testing "returns the id"
                (let [id (:id (:simulator (second response)))]
                  (is (re-matches uuid-re (str id)))))
              (testing "publishes event on activity feed"
                (let [{:keys [event data]} (async/<!! chan)]
                  (is (= :simulators/add (keyword event)))
                  (is (sims-match? [new-sim] [data]))))
              (testing "and when getting a list of simulators"
                (let [response (test.http/get "/api/simulators" content-type)]
                  (testing "returns a list of simulators"
                    (is (sims-match? (map simple (conj start-configs new-sim))
                                     (map simple (get-in response [1 :simulators]))))))))
            (testing "and when adding a bad simulator"
              (let [error-response (test.http/post "/api/simulators"
                                                   content-type
                                                   {:body {:simulator {:method :http/no-method}}})]
                (testing "returns an error response"
                  (is (test.http/client-error? error-response)))
                (testing "and when getting the list of simulators"
                  (let [response (test.http/get "/api/simulators" content-type)]
                    (testing "does not contain newest simulator"
                      (is (sims-match? (map simple (conj start-configs new-sim))
                                       (map simple (get-in response [1 :simulators])))))))))
            (testing "and when adding an existing simulator"
              (let [error (test.http/post "/api/simulators"
                                          content-type
                                          {:body {:simulator {:method   :http/get
                                                              :path     "/some/:different-param"
                                                              :response {:status 200
                                                                         :body   "some body"}}}})]
                (testing "returns an error response"
                  (is (test.http/client-error? error)))
                (testing "and when getting the list of simulators"
                  (let [response (test.http/get "/api/simulators" content-type)]
                    (testing "does not contain new simulator"
                      (is (sims-match? (map simple (conj start-configs new-sim))
                                       (map simple (get-in response [1 :simulators])))))))))
            (testing "and when initializing simulators again"
              (let [response (test.http/post "/api/simulators/init"
                                             content-type
                                             {:body {:simulators second-sims}})]
                (testing "returns a success response"
                  (is (test.http/success? response)))
                (testing "publishes event on activity feed"
                  (let [{:keys [event data]} (async/<!! chan)]
                    (is (= :simulators/init (keyword event)))
                    (is (sims-match? second-sims data))))
                (testing "and when getting a list of simulators"
                  (let [response (test.http/get "/api/simulators" content-type)]
                    (testing "returns a success response"
                      (is (test.http/success? response)))
                    (testing "returns a list of simulators"
                      (is (sims-match? (map simple second-sims)
                                       (map simple (get-in response [1 :simulators])))))))
                (testing "and when initializing bad simulators"
                  (let [response (test.http/post "/api/simulators/init"
                                                 content-type
                                                 {:body {:simulators (conj start-configs
                                                                           {:bad :sim})}})]
                    (testing "returns an error response"
                      (is (test.http/client-error? response)))
                    (testing "and when getting a list of simulators"
                      (let [response (test.http/get "/api/simulators" content-type)]
                        (testing "returns a success response"
                          (is (test.http/success? response)))
                        (testing "returns previous simulators"
                          (is (sims-match? (map simple second-sims)
                                           (map simple (get-in response [1 :simulators])))))))))))))
        (test.ws/close! ws)
        (async/close! chan)))))

(deftest ^:integration http-simulator-test
  (testing "[simulator API]"
    (doseq [content-type ["application/edn" "application/transit" "application/json"]]
      (testing "with simulators initialized"
        (let [response (test.http/post "/api/simulators/init" content-type {:body {:simulators start-configs}})]
          (is (test.http/success? response)))
        (let [chan (async/chan 64)
              ws (test.ws/connect "/api/simulators/activity"
                                  :query-params {:accept content-type}
                                  :on-msg (partial async/put! chan)
                                  :to-clj (content-type->parser content-type))]
          (testing "when sending request to the simulator"
            (let [response (test.http/get "/simulators/some/param?with=some&qps=included"
                                          content-type
                                          {:headers {:x-custom-request-header "some header"}})]
              (testing "responds as configured"
                (is (= :accepted (get response 2)))
                (is (= {:some "json"} (second response)))
                (is (= "application/json" (get-in response [3 :headers :content-type])))
                (is (= "some value" (get-in response [3 :headers :x-custom-header])))))
            (testing "publishes event on activity feed"
              (let [{:keys [event data]} (async/<!! chan)
                    {:keys [query-params route-params]} (:request data)]
                (is (= :simulators/receive (keyword event)))
                (is (= {:with "some" :qps "included"} query-params))
                (is (= {:url-param "param"} route-params))))
            (testing "and when getting the simulator's details"
              (let [response (test.http/get "/api/simulators/get/some/:url-param" content-type)]
                (testing "returns a success response"
                  (is (test.http/success? response)))
                (testing "includes request data"
                  (let [requests (get-in response [1 :simulator :requests])
                        {:keys [query-params route-params headers]} (first requests)]
                    (is (= 1 (count requests)))
                    (is (= {:with "some" :qps "included"} query-params))
                    (is (= {:url-param "param"} route-params))
                    (is (= "some header"
                           (:x-custom-request-header headers)))))
                (testing "and when the details are requested by id"
                  (let [id (get-in response [1 :simulator :id])
                        response-by-id (test.http/get (str "/api/simulators/" id) content-type)]
                    (is (= (second response-by-id) (second response)))))))
            (testing "and when varying request data"
              (let [response (test.http/get "/simulators/some/other-param?filter=things" content-type)]
                (testing "responds as configured"
                  (is (= {:some "json"} (second response))))
                (testing "publishes event on activity feed"
                  (let [{:keys [query-params route-params]} (get-in (async/<!! chan)
                                                                    [:data :request])]
                    (is (= {:filter "things"} query-params))
                    (is (= {:url-param "other-param"} route-params))))
                (testing "and when getting the simulator's details"
                  (let [{:keys [query-params route-params]}
                        (get-in (test.http/get "/api/simulators/get/some/:url-param" content-type)
                                [1 :simulator :requests 1])]
                    (testing "includes request data"
                      (is (= {:filter "things"} query-params))
                      (is (= {:url-param "other-param"} route-params)))))))
            (testing "and when updating the simulator"
              (let [body-f (content-type->stringify content-type)
                    new-config {:delay    1000
                                :response {:body    (body-f {:new "body"})
                                           :headers {:content-type content-type}}}]
                (test.http/patch "/api/simulators/get/some/:url-param"
                                 content-type
                                 {:body {:action :simulators/change
                                         :config new-config}})
                (testing "publishes event on activity feed"
                  (let [{:keys [event data]} (async/<!! chan)]
                    (is (= :simulators/change (keyword event)))
                    (is (= (-> data
                               (:config)
                               (update :method keyword))
                           (maps/deep-merge (first start-configs) new-config)))))
                (testing "and when sending request to the simulator"
                  (let [now (.getTime (Date.))
                        response (test.http/get "/simulators/some/param" content-type)
                        elapsed (- (.getTime (Date.)) now)]
                    (testing "responds with updated configuration"
                      (is (test.http/success? response))
                      (is (>= elapsed 1000))
                      (is (= {:new "body"} (second response))))
                    (testing "publishes event on activity feed"
                      (let [event (:event (async/<!! chan))]
                        (is (= :simulators/receive (keyword event)))))
                    (testing "and when getting the simulator's details"
                      (let [[_ {{:keys [config requests]} :simulator}]
                            (test.http/get "/api/simulators/get/some/:url-param" content-type)]
                        (testing "includes updated details"
                          (is (= 1000 (:delay config)))
                          (is (= (body-f {:new "body"}) (get-in config [:response :body]))))
                        (testing "includes requests"
                          (is (= 3 (count requests))))))))
                (testing "and when resetting the requests"
                  (test.http/patch "/api/simulators/get/some/:url-param"
                                   content-type
                                   {:body {:action :simulators/reset-requests}})
                  (testing "publishes event on activity feed"
                    (let [{:keys [event data]} (async/<!! chan)]
                      (is (= :simulators/reset-requests (keyword event)))
                      (is (= {:method :http/get :path "/some/:url-param"}
                             (-> data
                                 (:config)
                                 (update :method keyword)
                                 (select-keys #{:method :path}))))))
                  (testing "and when getting the simulator's details"
                    (let [[_ {{:keys [config requests]} :simulator}]
                          (test.http/get "/api/simulators/get/some/:url-param" content-type)]
                      (testing "has no requests"
                        (is (empty? requests)))
                      (testing "still has updated response"
                        (is (= (body-f {:new "body"})
                               (get-in config [:response :body]))))))
                  (testing "and when sending a request"
                    (test.http/get "/simulators/some/final-request" content-type)
                    (testing "publishes event on activity feed"
                      (let [event (:event (async/<!! chan))]
                        (is (= :simulators/receive (keyword event)))))
                    (testing "and when resetting the response"
                      (test.http/patch "/api/simulators/get/some/:url-param"
                                       content-type
                                       {:body {:action :http/reset-response}})
                      (testing "publishes event on activity feed"
                        (let [event (:event (async/<!! chan))]
                          (is (= :http/reset-response (keyword event)))))
                      (testing "and when getting the simulator's details"
                        (let [[_ {{:keys [config requests]} :simulator}]
                              (test.http/get "/api/simulators/get/some/:url-param"
                                             content-type)
                              {:keys [body headers]} (:response config)]
                          (testing "has the request"
                            (is (= 1 (count requests)))
                            (is (= {:url-param "final-request"}
                                   (:route-params (first requests)))))
                          (testing "has original response config"
                            (is (= "application/json"
                                   (:content-type headers)))
                            (is (= (json/stringify {:some :json}) body))
                            (is (= 1 (:delay config))))))))))))
          (testing "when updating the simulator"
            (test.http/patch "/api/simulators/get/some/:url-param"
                             content-type
                             {:body {:action :simulators/change
                                     :config {:body    (transit/stringify {:update :again})
                                              :headers {:content-type "application/transit"}}}})
            (testing "publishes event on activity feed"
              (let [event (:event (async/<!! chan))]
                (is (= :simulators/change (keyword event)))))
            (testing "and when updating a different simulator"
              (test.http/patch "/api/simulators/post/some/path"
                               content-type
                               {:body {:action :simulators/change
                                       :config {:response {:status 403
                                                           :body   (pr-str {:message "not authorized"})}}}})
              (testing "publishes event on activity feed"
                (let [{:keys [event data]} (async/<!! chan)]
                  (is (= :simulators/change (keyword event)))
                  (is (= :http/post (keyword (get-in data [:config :method]))))
                  (is (= "/some/path" (get-in data [:config :path])))
                  data)
                (testing "and when sending a request to that simulator"
                  (test.http/post "/simulators/some/path"
                                  "application/edn"
                                  {:body (pr-str [:new :edn :body])})
                  (testing "publishes event on activity feed"
                    (let [{:keys [event data]} (async/<!! chan)]
                      (is (= :simulators/receive (keyword event)))
                      (is (= :http/post (keyword (get-in data [:simulator :config :method]))))
                      (is (= "/some/path" (get-in data [:simulator :config :path])))))
                  (testing "and when sending a request to original simulator"
                    (test.http/get "/simulators/some/things" content-type)
                    (testing "publishes event on activity feed"
                      (let [event (:event (async/<!! chan))]
                        (is (= :simulators/receive (keyword event)))))
                    (testing "and when resetting the simulator"
                      (test.http/patch "/api/simulators/get/some/:url-param"
                                       content-type
                                       {:body {:action :simulators/reset}})
                      (testing "publishes event on activity feed"
                        (let [event (:event (async/<!! chan))]
                          (is (= :simulators/reset (keyword event)))))
                      (testing "and when getting the simulator's details"
                        (let [[_ {{:keys [requests config]} :simulator}]
                              (test.http/get "/api/simulators/get/some/:url-param"
                                             content-type)]
                          (testing "has no requests"
                            (is (empty? requests)))
                          (testing "has original response config"
                            (is (= 202 (get-in config [:response :status])))
                            (is (= (json/stringify {:some :json})
                                   (get-in config [:response :body])))
                            (is (= {:content-type    "application/json"
                                    :x-custom-header "some value"}
                                   (get-in config [:response :headers]))))
                          (testing "has original delay value"
                            (is (= 1 (:delay config))))))
                      (testing "and when requesting the other simulator's details"
                        (let [[_ {{:keys [requests config]} :simulator}]
                              (test.http/get "/api/simulators/post/some/path" content-type)]
                          (testing "has the updated response"
                            (is (= 403 (get-in config [:response :status])))
                            (is (= (pr-str {:message "not authorized"}) (get-in config [:response :body]))))
                          (testing "has the received request"
                            (is (= 1 (count requests)))
                            (is (= (pr-str [:new :edn :body]) (:body (first requests)))))))))))))
          (testing "when updating both simulators"
            (test.http/patch "/api/simulators/get/some/:url-param"
                             content-type
                             {:body {:action :simulators/change
                                     :config {:response {:body (json/stringify [:GET "some" "things"])}}}})
            (test.http/patch "/api/simulators/post/some/path"
                             content-type
                             {:body {:action :simulators/change
                                     :config {:response {:body (pr-str [:POST "some" "things"])}}}})
            (testing "and when sending a request to each simulator"
              (test.http/get "/simulators/some/id-for-things" content-type)
              (test.http/post "/simulators/some/path" content-type {:body [:new "thing"]})
              (testing "publishes events on activity feed"
                (let [update-get (async/<!! chan)
                      update-post (async/<!! chan)
                      get-request (async/<!! chan)
                      post-request (async/<!! chan)]
                  (is (= (json/stringify [:GET "some" "things"])
                         (get-in update-get [:data :config :response :body])))
                  (is (= (pr-str [:POST "some" "things"])
                         (get-in update-post [:data :config :response :body])))
                  (is (= "id-for-things" (get-in get-request [:data :request :route-params :url-param])))
                  (is (pr-str [:new "thing"])
                      (get-in post-request [:data :request :body]))))
              (testing "and when resetting all simulators"
                (test.http/delete "/api/simulators/reset" content-type)
                (testing "publishes event on activity feed"
                  (let [{:keys [event data]} (async/<!! chan)]
                    (is (= :simulators/reset-all (keyword event)))
                    (is (= (set start-configs)
                           (set (map (comp #(update % :method keyword) :config) data))))))
                (testing "and when getting each simulator's details"
                  (let [[_ get-sim] (test.http/get "/api/simulators/get/some/:url-param" content-type)
                        [_ post-sim] (test.http/get "/api/simulators/post/some/path" content-type)]
                    (testing "has no requests for either simulator"
                      (is (empty? (:requests get-sim)))
                      (is (empty? (:requests post-sim))))
                    (testing "has original response for both simulators"
                      (is (= "{\"some\":\"json\"}" (get-in get-sim [:simulator :config :response :body])))
                      (is (= "{:some :edn}" (get-in post-sim [:simulator :config :response :body])))))))))
          (testing "when deleting a simulator"
            (test.http/delete "/api/simulators/get/some/:url-param" content-type)
            (testing "publishes event on activity feed"
              (let [{:keys [event data]} (async/<!! chan)]
                (is (= :simulators/delete (keyword event)))
                (is (= (-> data
                           (:config)
                           (update :method keyword)
                           (select-keys #{:method :path}))
                       {:method :http/get :path "/some/:url-param"}))
                (testing "and when getting a list of simulators"
                  (let [[_ sims] (test.http/get "/api/simulators" content-type)]
                    (testing "does not include deleted simulator"
                      (is (= 1 (count (:simulators sims))))
                      (is (= {:method :http/post :path "/some/path"}
                             (-> (first (:simulators sims))
                                 (:config)
                                 (update :method keyword)
                                 (select-keys #{:method :path})))))
                    (testing "and when sending a request to deleted simulator"
                      (let [response (test.http/get "/simulators/some/missing" content-type)]
                        (testing "returns a client error"
                          (is (test.http/client-error? response)))
                        (testing "does not publish event on activity feed"
                          (async/put! chan ::nothing)
                          (is (= ::nothing (async/<!! chan)))))))))))
          (test.ws/close! ws)
          (async/close! chan))))))
