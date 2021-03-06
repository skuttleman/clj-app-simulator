(ns integration.tests.simulators.http
  (:require
    [clojure.core.async :as async]
    [clojure.edn :as edn]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [com.ben-allred.app-simulator.api.utils.specs :as specs]
    [com.ben-allred.app-simulator.core :as sim-core]
    [com.ben-allred.app-simulator.utils.json :as json]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [com.ben-allred.app-simulator.utils.transit :as transit]
    [integration.utils.chans :as chans]
    [integration.utils.fixtures :as fixtures]
    [integration.utils.http :as test.http]
    [integration.utils.ws :as test.ws])
  (:import
    (clojure.lang ExceptionInfo)
    (java.util Date)))

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
  (comp #(:config %) #(specs/conform :simulator.http/config %)))

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

(deftest ^:integration http-simulators-web-init-test
  (testing "[simulators Web API]"
    (doseq [content-type ["application/edn" "application/transit" "application/json"]]
      (let [chan (async/chan 64)
            ws (test.ws/connect "/api/simulators/activity"
                                :query-params {:accept content-type}
                                :on-msg (fn [_ msg] (async/put! chan msg))
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
            (let [{:keys [event data]} (chans/<⏰!! chan)]
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
                (let [{:keys [event data]} (chans/<⏰!! chan)]
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
                  (let [{:keys [event data]} (chans/<⏰!! chan)]
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

(deftest ^:integration http-simulators-api-init-test
  (testing "[simulators Clojure API]"
    (doseq [content-type ["application/edn" "application/transit" "application/json"]]
      (let [chan (async/chan 64)
            ws (test.ws/connect "/api/simulators/activity"
                                :query-params {:accept content-type}
                                :on-msg (fn [_ msg] (async/put! chan msg))
                                :to-clj (content-type->parser content-type))]
        (testing (str "when saving initial simulators with content-type: " (pr-str content-type))
          (let [sims (sim-core/init-simulators start-configs)]
            (testing "returns ids"
              (let [ids (map :id sims)]
                (is (= 2 (count ids)))
                (is (every? uuid? ids)))))

          (testing "publishes event on activity feed"
            (let [{:keys [event data]} (chans/<⏰!! chan)]
              (is (= :simulators/init (keyword event)))
              (is (sims-match? start-configs data))))
          (testing "and when getting a list of simulators"
            (let [sims (sim-core/list-simulators)]
              (testing "returns a list of simulators"
                (is (sims-match? (map simple start-configs)
                                 (map simple sims))))))
          (testing "and when adding a simulator"
            (let [sim (sim-core/create-simulator new-sim)]
              (testing "returns the id"
                (let [id (:id sim)]
                  (is (uuid? id))))
              (testing "publishes event on activity feed"
                (let [{:keys [event data]} (chans/<⏰!! chan)]
                  (is (= :simulators/add (keyword event)))
                  (is (sims-match? [new-sim] [data]))))
              (testing "and when getting a list of simulators"
                (let [sims (sim-core/list-simulators)]
                  (testing "returns a list of simulators"
                    (is (sims-match? (map simple (conj start-configs new-sim))
                                     (map simple sims)))))))
            (testing "and when adding a bad simulator"
              (is (thrown? ExceptionInfo (sim-core/create-simulator {:method :http/no-method})))
              (testing "and when getting the list of simulators"
                (let [sims (sim-core/list-simulators)]
                  (testing "does not contain newest simulator"
                    (is (sims-match? (map simple (conj start-configs new-sim))
                                     (map simple sims)))))))
            (testing "and when adding an existing simulator"
              (is (thrown? ExceptionInfo (sim-core/create-simulator {:method   :http/get
                                                                     :path     "/some/:different-param"
                                                                     :response {:status 200
                                                                                :body   "some body"}})))
              (testing "and when getting the list of simulators"
                (let [sims (sim-core/list-simulators)]
                  (testing "does not contain new simulator"
                    (is (sims-match? (map simple (conj start-configs new-sim))
                                     (map simple sims)))))))
            (testing "and when initializing simulators again"
              (sim-core/init-simulators second-sims)
              (testing "publishes event on activity feed"
                (let [{:keys [event data]} (chans/<⏰!! chan)]
                  (is (= :simulators/init (keyword event)))
                  (is (sims-match? second-sims data))))
              (testing "and when getting a list of simulators"
                (let [sims (sim-core/list-simulators)]
                  (testing "returns a list of simulators"
                    (is (sims-match? (map simple second-sims)
                                     (map simple sims))))))
              (testing "and when initializing bad simulators"
                (is (thrown? ExceptionInfo (sim-core/init-simulators (conj start-configs
                                                                           {:bad :sim}))))
                (testing "and when getting a list of simulators"
                  (let [sims (sim-core/list-simulators)]
                    (testing "returns previous simulators"
                      (is (sims-match? (map simple second-sims)
                                       (map simple sims))))))))))
        (test.ws/close! ws)
        (async/close! chan)))))

(deftest ^:integration http-simulator-web-test
  (testing "[simulator Web API]"
    (doseq [content-type ["application/edn" "application/transit" "application/json"]]
      (testing "with simulators initialized"
        (let [[_ body :as response] (test.http/post "/api/simulators/init" content-type {:body {:simulators start-configs}})
              sim->id (into {}
                            (map (juxt (comp (juxt (comp keyword name keyword :method)
                                                   :path)
                                             :config)
                                       :id))
                            (:simulators body))]
          (is (test.http/success? response))
          (let [chan (async/chan 64)
                ws (test.ws/connect "/api/simulators/activity"
                                    :query-params {:accept content-type}
                                    :on-msg (fn [_ msg] (async/put! chan msg))
                                    :to-clj (content-type->parser content-type))]
            (testing "when sending request to the simulator"
              (let [response (test.http/get "/simulators/some/param?with=some&qps=included"
                                            content-type
                                            {:headers {:x-custom-request-header "some header"}})]
                (testing "responds as configured"
                  (is (= :http.status/accepted (get response 2)))
                  (is (= {:some "json"} (second response)))
                  (is (= "application/json" (get-in response [3 :headers :content-type])))
                  (is (= "some value" (get-in response [3 :headers :x-custom-header])))))
              (testing "publishes event on activity feed"
                (let [{:keys [event data]} (chans/<⏰!! chan)
                      {:keys [query-params route-params]} (:request data)]
                  (is (= :simulators/receive (keyword event)))
                  (is (= {:with "some" :qps "included"} query-params))
                  (is (= {:url-param "param"} route-params))))
              (testing "and when getting the simulator's details"
                (let [response (test.http/get (str "/api/simulators/" (sim->id [:get "/some/:url-param"])) content-type)]
                  (testing "returns a success response"
                    (is (test.http/success? response)))
                  (testing "includes request data"
                    (let [requests (get-in response [1 :simulator :requests])
                          {:keys [query-params route-params headers]} (first requests)]
                      (is (= 1 (count requests)))
                      (is (= {:with "some" :qps "included"} query-params))
                      (is (= {:url-param "param"} route-params))
                      (is (= "some header"
                             (:x-custom-request-header headers)))))))
              (testing "and when varying request data"
                (let [response (test.http/get "/simulators/some/other-param?filter=things" content-type)]
                  (testing "responds as configured"
                    (is (= {:some "json"} (second response))))
                  (testing "publishes event on activity feed"
                    (let [{:keys [query-params route-params]} (get-in (chans/<⏰!! chan)
                                                                      [:data :request])]
                      (is (= {:filter "things"} query-params))
                      (is (= {:url-param "other-param"} route-params))))
                  (testing "and when getting the simulator's details"
                    (let [{:keys [query-params route-params]}
                          (get-in (test.http/get (str "/api/simulators/" (sim->id [:get "/some/:url-param"])) content-type)
                                  [1 :simulator :requests 1])]
                      (testing "includes request data"
                        (is (= {:filter "things"} query-params))
                        (is (= {:url-param "other-param"} route-params)))))))
              (testing "and when updating the simulator"
                (let [body-f (content-type->stringify content-type)
                      new-config {:delay    1000
                                  :response {:body    (body-f {:new "body"})
                                             :headers {:content-type content-type}}}]
                  (test.http/patch (str "/api/simulators/" (sim->id [:get "/some/:url-param"]))
                                   content-type
                                   {:body {:action :simulators/change
                                           :config new-config}})
                  (testing "publishes event on activity feed"
                    (let [{:keys [event data]} (chans/<⏰!! chan)]
                      (is (= :simulators/change (keyword event)))
                      (is (= (-> data
                                 (get-in [:simulator :config])
                                 (update :method keyword))
                             (-> start-configs
                                 (first)
                                 (update :response merge (:response new-config))
                                 (assoc :delay 1000))))))
                  (testing "and when sending request to the simulator"
                    (let [now (.getTime (Date.))
                          response (test.http/get "/simulators/some/param" content-type)
                          elapsed (- (.getTime (Date.)) now)]
                      (testing "responds with updated configuration"
                        (is (test.http/success? response))
                        (is (>= elapsed 1000))
                        (is (= {:new "body"} (second response))))
                      (testing "publishes event on activity feed"
                        (let [event (:event (chans/<⏰!! chan))]
                          (is (= :simulators/receive (keyword event)))))
                      (testing "and when getting the simulator's details"
                        (let [[_ {{:keys [config requests]} :simulator}]
                              (test.http/get (str "/api/simulators/" (sim->id [:get "/some/:url-param"])) content-type)]
                          (testing "includes updated details"
                            (is (= 1000 (:delay config)))
                            (is (= (body-f {:new "body"}) (get-in config [:response :body]))))
                          (testing "includes requests"
                            (is (= 3 (count requests))))))))
                  (testing "and when resetting the requests"
                    (test.http/patch (str "/api/simulators/" (sim->id [:get "/some/:url-param"]))
                                     content-type
                                     {:body {:action :simulators/reset :type :http/requests}})
                    (testing "publishes event on activity feed"
                      (let [{:keys [event data]} (chans/<⏰!! chan)]
                        (is (= :simulators/reset (keyword event)))
                        (is (= {:method :http/get :path "/some/:url-param"}
                               (-> data
                                   (get-in [:simulator :config])
                                   (update :method keyword)
                                   (select-keys #{:method :path}))))))
                    (testing "and when getting the simulator's details"
                      (let [[_ {{:keys [config requests]} :simulator}]
                            (test.http/get (str "/api/simulators/" (sim->id [:get "/some/:url-param"])) content-type)]
                        (testing "has no requests"
                          (is (empty? requests)))
                        (testing "still has updated response"
                          (is (= (body-f {:new "body"})
                                 (get-in config [:response :body]))))))
                    (testing "and when sending a request"
                      (test.http/get "/simulators/some/final-request" content-type)
                      (testing "publishes event on activity feed"
                        (let [event (:event (chans/<⏰!! chan))]
                          (is (= :simulators/receive (keyword event)))))
                      (testing "and when resetting the response"
                        (test.http/patch (str "/api/simulators/" (sim->id [:get "/some/:url-param"]))
                                         content-type
                                         {:body {:action :simulators/reset :type :http/response}})
                        (testing "publishes event on activity feed"
                          (let [event (:event (chans/<⏰!! chan))]
                            (is (= :simulators/reset (keyword event)))))
                        (testing "and when getting the simulator's details"
                          (let [[_ {{:keys [config requests]} :simulator :as body}]
                                (test.http/get (str "/api/simulators/" (sim->id [:get "/some/:url-param"]))
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
              (test.http/patch (str "/api/simulators/" (sim->id [:get "/some/:url-param"]))
                               content-type
                               {:body {:action :simulators/change
                                       :config {:body    (transit/stringify {:update :again})
                                                :headers {:content-type "application/transit"}}}})
              (testing "publishes event on activity feed"
                (let [event (:event (chans/<⏰!! chan))]
                  (is (= :simulators/change (keyword event)))))
              (testing "and when updating a different simulator"
                (test.http/patch (str "/api/simulators/" (sim->id [:post "/some/path"]))
                                 content-type
                                 {:body {:action :simulators/change
                                         :config {:response {:status 403
                                                             :body   (pr-str {:message "not authorized"})}}}})
                (testing "publishes event on activity feed"
                  (let [{:keys [event data]} (chans/<⏰!! chan)]
                    (is (= :simulators/change (keyword event)))
                    (is (= :http/post (keyword (get-in data [:simulator :config :method]))))
                    (is (= "/some/path" (get-in data [:simulator :config :path])))
                    data)
                  (testing "and when sending a request to that simulator"
                    (test.http/post "/simulators/some/path"
                                    "application/edn"
                                    {:body (pr-str [:new :edn :body])})
                    (testing "publishes event on activity feed"
                      (let [{:keys [event data]} (chans/<⏰!! chan)]
                        (is (= :simulators/receive (keyword event)))
                        (is (= :http/post (keyword (get-in data [:simulator :config :method]))))
                        (is (= "/some/path" (get-in data [:simulator :config :path])))))
                    (testing "and when sending a request to original simulator"
                      (test.http/get "/simulators/some/things" content-type)
                      (testing "publishes event on activity feed"
                        (let [event (:event (chans/<⏰!! chan))]
                          (is (= :simulators/receive (keyword event)))))
                      (testing "and when resetting the simulator"
                        (test.http/patch (str "/api/simulators/" (sim->id [:get "/some/:url-param"]))
                                         content-type
                                         {:body {:action :simulators/reset}})
                        (testing "publishes event on activity feed"
                          (let [event (:event (chans/<⏰!! chan))]
                            (is (= :simulators/reset (keyword event)))))
                        (testing "and when getting the simulator's details"
                          (let [[_ {{:keys [requests config]} :simulator}]
                                (test.http/get (str "/api/simulators/" (sim->id [:get "/some/:url-param"]))
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
                                (test.http/get (str "/api/simulators/" (sim->id [:post "/some/path"])) content-type)]
                            (testing "has the updated response"
                              (is (= 403 (get-in config [:response :status])))
                              (is (= (pr-str {:message "not authorized"}) (get-in config [:response :body]))))
                            (testing "has the received request"
                              (is (= 1 (count requests)))
                              (is (= (pr-str [:new :edn :body]) (:body (first requests)))))))))))))
            (testing "when updating both simulators"
              (test.http/patch (str "/api/simulators/" (sim->id [:get "/some/:url-param"]))
                               content-type
                               {:body {:action :simulators/change
                                       :config {:response {:body (json/stringify [:GET "some" "things"])}}}})
              (test.http/patch (str "/api/simulators/" (sim->id [:post "/some/path"]))
                               content-type
                               {:body {:action :simulators/change
                                       :config {:response {:body (pr-str [:POST "some" "things"])}}}})
              (testing "and when sending a request to each simulator"
                (test.http/get "/simulators/some/id-for-things" content-type)
                (test.http/post "/simulators/some/path" content-type {:body [:new "thing"]})
                (testing "publishes events on activity feed"
                  (let [update-get (chans/<⏰!! chan)
                        update-post (chans/<⏰!! chan)
                        get-request (chans/<⏰!! chan)
                        post-request (chans/<⏰!! chan)]
                    (is (= (json/stringify [:GET "some" "things"])
                           (get-in update-get [:data :simulator :config :response :body])))
                    (is (= (pr-str [:POST "some" "things"])
                           (get-in update-post [:data :simulator :config :response :body])))
                    (is (= "id-for-things" (get-in get-request [:data :request :route-params :url-param])))
                    (is (pr-str [:new "thing"])
                        (get-in post-request [:data :request :body]))))
                (testing "and when resetting all simulators"
                  (test.http/delete "/api/simulators/reset" content-type)
                  (testing "publishes event on activity feed"
                    (let [{:keys [event data]} (chans/<⏰!! chan)]
                      (is (= :simulators/reset-all (keyword event)))
                      (is (= (set start-configs)
                             (set (map (comp #(update % :method keyword) :config) (:simulators data)))))))
                  (testing "and when getting each simulator's details"
                    (let [[_ get-sim] (test.http/get (str "/api/simulators/" (sim->id [:get "/some/:url-param"])) content-type)
                          [_ post-sim] (test.http/get (str "/api/simulators/" (sim->id [:post "/some/path"])) content-type)]
                      (testing "has no requests for either simulator"
                        (is (empty? (get-in get-sim [:simulator :requests])))
                        (is (empty? (get-in post-sim [:simulator :requests]))))
                      (testing "has original response for both simulators"
                        (is (= "{\"some\":\"json\"}" (get-in get-sim [:simulator :config :response :body])))
                        (is (= "{:some :edn}" (get-in post-sim [:simulator :config :response :body])))))))))
            (testing "when deleting a simulator"
              (test.http/delete (str "/api/simulators/" (sim->id [:get "/some/:url-param"])) content-type)
              (testing "publishes event on activity feed"
                (let [{:keys [event data]} (chans/<⏰!! chan)]
                  (is (= :simulators/delete (keyword event)))
                  (is (= (-> data
                             (get-in [:simulator :config])
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
                            (is (= ::nothing (chans/<⏰!! chan)))))))))))
            (test.ws/close! ws)
            (async/close! chan)))))))

(deftest ^:integration http-simulator-api-test
  (testing "[simulator Clojure API]"
    (doseq [content-type ["application/edn" "application/transit" "application/json"]]
      (testing "with simulators initialized"
        (let [sims (sim-core/init-simulators start-configs)
              sim->id (into {}
                            (map (juxt (comp (juxt (comp keyword name keyword :method)
                                                   :path)
                                             :config)
                                       :id))
                            sims)]
          (let [chan (async/chan 64)
                ws (test.ws/connect "/api/simulators/activity"
                                    :query-params {:accept content-type}
                                    :on-msg (fn [_ msg] (async/put! chan msg))
                                    :to-clj (content-type->parser content-type))]
            (testing "when sending request to the simulator"
              (let [response (test.http/get "/simulators/some/param?with=some&qps=included"
                                            content-type
                                            {:headers {:x-custom-request-header "some header"}})]
                (testing "responds as configured"
                  (is (= :http.status/accepted (get response 2)))
                  (is (= {:some "json"} (second response)))
                  (is (= "application/json" (get-in response [3 :headers :content-type])))
                  (is (= "some value" (get-in response [3 :headers :x-custom-header])))))
              (testing "publishes event on activity feed"
                (let [{:keys [event data]} (chans/<⏰!! chan)
                      {:keys [query-params route-params]} (:request data)]
                  (is (= :simulators/receive (keyword event)))
                  (is (= {:with "some" :qps "included"} query-params))
                  (is (= {:url-param "param"} route-params))))
              (testing "and when getting the simulator's details"
                (let [details (sim-core/details (sim->id [:get "/some/:url-param"]))]
                  (testing "includes request data"
                    (let [requests (:requests details)
                          {:keys [query-params route-params headers]} (first requests)]
                      (is (= 1 (count requests)))
                      (is (= {:with "some" :qps "included"} query-params))
                      (is (= {:url-param "param"} route-params))
                      (is (= "some header"
                             (:x-custom-request-header headers)))))))
              (testing "and when varying request data"
                (let [response (test.http/get "/simulators/some/other-param?filter=things" content-type)]
                  (testing "responds as configured"
                    (is (= {:some "json"} (second response))))
                  (testing "publishes event on activity feed"
                    (let [{:keys [query-params route-params]} (get-in (chans/<⏰!! chan)
                                                                      [:data :request])]
                      (is (= {:filter "things"} query-params))
                      (is (= {:url-param "other-param"} route-params))))
                  (testing "and when getting the simulator's details"
                    (let [{:keys [query-params route-params]}
                          (second (:requests (sim-core/details (sim->id [:get "/some/:url-param"]))))]
                      (testing "includes request data"
                        (is (= {:filter "things"} query-params))
                        (is (= {:url-param "other-param"} route-params)))))))
              (testing "and when updating the simulator"
                (let [body-f (content-type->stringify content-type)
                      new-config {:delay    1000
                                  :response {:body    (body-f {:new "body"})
                                             :headers {:content-type content-type}}}]
                  (sim-core/change! (sim->id [:get "/some/:url-param"]) new-config)
                  (testing "publishes event on activity feed"
                    (let [{:keys [event data]} (chans/<⏰!! chan)]
                      (is (= :simulators/change (keyword event)))
                      (is (= (-> data
                                 (get-in [:simulator :config])
                                 (update :method keyword))
                             (-> start-configs
                                 (first)
                                 (update :response merge (:response new-config))
                                 (assoc :delay 1000))))))
                  (testing "and when sending request to the simulator"
                    (let [now (.getTime (Date.))
                          response (test.http/get "/simulators/some/param" content-type)
                          elapsed (- (.getTime (Date.)) now)]
                      (testing "responds with updated configuration"
                        (is (>= elapsed 1000))
                        (is (= {:new "body"} (second response))))
                      (testing "publishes event on activity feed"
                        (let [event (:event (chans/<⏰!! chan))]
                          (is (= :simulators/receive (keyword event)))))
                      (testing "and when getting the simulator's details"
                        (let [{:keys [config requests]} (sim-core/details (sim->id [:get "/some/:url-param"]))]
                          (testing "includes updated details"
                            (is (= 1000 (:delay config)))
                            (is (= (body-f {:new "body"}) (get-in config [:response :body]))))
                          (testing "includes requests"
                            (is (= 3 (count requests))))))))
                  (testing "and when resetting the requests"
                    (sim-core/partially-reset! (sim->id [:get "/some/:url-param"]) :http/requests)
                    (testing "publishes event on activity feed"
                      (let [{:keys [event data]} (chans/<⏰!! chan)]
                        (is (= :simulators/reset (keyword event)))
                        (is (= {:method :http/get :path "/some/:url-param"}
                               (-> data
                                   (get-in [:simulator :config])
                                   (update :method keyword)
                                   (select-keys #{:method :path}))))))
                    (testing "and when getting the simulator's details"
                      (let [{:keys [config requests]} (sim-core/details (sim->id [:get "/some/:url-param"]))]
                        (testing "has no requests"
                          (is (empty? requests)))
                        (testing "still has updated response"
                          (is (= (body-f {:new "body"})
                                 (get-in config [:response :body]))))))
                    (testing "and when sending a request"
                      (test.http/get "/simulators/some/final-request" content-type)
                      (testing "publishes event on activity feed"
                        (let [event (:event (chans/<⏰!! chan))]
                          (is (= :simulators/receive (keyword event)))))
                      (testing "and when resetting the response"
                        (sim-core/partially-reset! (sim->id [:get "/some/:url-param"]) :http/response)
                        (testing "publishes event on activity feed"
                          (let [event (:event (chans/<⏰!! chan))]
                            (is (= :simulators/reset (keyword event)))))
                        (testing "and when getting the simulator's details"
                          (let [{:keys [config requests]} (sim-core/details (sim->id [:get "/some/:url-param"]))
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
              (sim-core/change! (sim->id [:get "/some/:url-param"])
                                {:body    (transit/stringify {:update :again})
                                 :headers {:content-type "application/transit"}})
              (testing "publishes event on activity feed"
                (let [event (:event (chans/<⏰!! chan))]
                  (is (= :simulators/change (keyword event)))))
              (testing "and when updating a different simulator"
                (sim-core/change! (sim->id [:post "/some/path"])
                                  {:response {:status 403
                                              :body   (pr-str {:message "not authorized"})}})
                (testing "publishes event on activity feed"
                  (let [{:keys [event data]} (chans/<⏰!! chan)]
                    (is (= :simulators/change (keyword event)))
                    (is (= :http/post (keyword (get-in data [:simulator :config :method]))))
                    (is (= "/some/path" (get-in data [:simulator :config :path])))
                    data)
                  (testing "and when sending a request to that simulator"
                    (test.http/post "/simulators/some/path"
                                    "application/edn"
                                    {:body (pr-str [:new :edn :body])})
                    (testing "publishes event on activity feed"
                      (let [{:keys [event data]} (chans/<⏰!! chan)]
                        (is (= :simulators/receive (keyword event)))
                        (is (= :http/post (keyword (get-in data [:simulator :config :method]))))
                        (is (= "/some/path" (get-in data [:simulator :config :path])))))
                    (testing "and when sending a request to original simulator"
                      (test.http/get "/simulators/some/things" content-type)
                      (testing "publishes event on activity feed"
                        (let [event (:event (chans/<⏰!! chan))]
                          (is (= :simulators/receive (keyword event)))))
                      (testing "and when resetting the simulator"
                        (sim-core/reset! (sim->id [:get "/some/:url-param"]))
                        (testing "publishes event on activity feed"
                          (let [event (:event (chans/<⏰!! chan))]
                            (is (= :simulators/reset (keyword event)))))
                        (testing "and when getting the simulator's details"
                          (let [{:keys [requests config]} (sim-core/details (sim->id [:get "/some/:url-param"]))]
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
                          (let [{:keys [requests config]} (sim-core/details (sim->id [:post "/some/path"]))]
                            (testing "has the updated response"
                              (is (= 403 (get-in config [:response :status])))
                              (is (= (pr-str {:message "not authorized"}) (get-in config [:response :body]))))
                            (testing "has the received request"
                              (is (= 1 (count requests)))
                              (is (= (pr-str [:new :edn :body]) (:body (first requests)))))))))))))
            (testing "when updating both simulators"
              (sim-core/change! (sim->id [:get "/some/:url-param"])
                                {:response {:body (json/stringify [:GET "some" "things"])}})
              (sim-core/change! (sim->id [:post "/some/path"])
                                {:response {:body (pr-str [:POST "some" "things"])}})
              (testing "and when sending a request to each simulator"
                (test.http/get "/simulators/some/id-for-things" content-type)
                (test.http/post "/simulators/some/path" content-type {:body [:new "thing"]})
                (testing "publishes events on activity feed"
                  (let [update-get (chans/<⏰!! chan)
                        update-post (chans/<⏰!! chan)
                        get-request (chans/<⏰!! chan)
                        post-request (chans/<⏰!! chan)]
                    (is (= (json/stringify [:GET "some" "things"])
                           (get-in update-get [:data :simulator :config :response :body])))
                    (is (= (pr-str [:POST "some" "things"])
                           (get-in update-post [:data :simulator :config :response :body])))
                    (is (= "id-for-things" (get-in get-request [:data :request :route-params :url-param])))
                    (is (pr-str [:new "thing"])
                        (get-in post-request [:data :request :body]))))
                (testing "and when resetting all simulators"
                  (sim-core/reset!)
                  (testing "publishes event on activity feed"
                    (let [{:keys [event data]} (chans/<⏰!! chan)]
                      (is (= :simulators/reset-all (keyword event)))
                      (is (= (set start-configs)
                             (set (map (comp #(update % :method keyword) :config) (:simulators data)))))))
                  (testing "and when getting each simulator's details"
                    (let [get-sim (sim-core/details (sim->id [:get "/some/:url-param"]))
                          post-sim (sim-core/details (sim->id [:post "/some/path"]))]
                      (testing "has no requests for either simulator"
                        (is (empty? (:requests get-sim)))
                        (is (empty? (:requests post-sim))))
                      (testing "has original response for both simulators"
                        (is (= "{\"some\":\"json\"}" (get-in get-sim [:config :response :body])))
                        (is (= "{:some :edn}" (get-in post-sim [:config :response :body])))))))))
            (testing "when deleting a simulator"
              (sim-core/delete-simulators! (sim->id [:get "/some/:url-param"]))
              (testing "publishes event on activity feed"
                (let [{:keys [event data]} (chans/<⏰!! chan)]
                  (is (= :simulators/delete (keyword event)))
                  (is (= (-> data
                             (get-in [:simulator :config])
                             (update :method keyword)
                             (select-keys #{:method :path}))
                         {:method :http/get :path "/some/:url-param"}))
                  (testing "and when getting a list of simulators"
                    (let [sims (sim-core/list-simulators)]
                      (testing "does not include deleted simulator"
                        (is (= 1 (count sims)))
                        (is (= {:method :http/post :path "/some/path"}
                               (-> sims
                                   (first)
                                   (:config)
                                   (update :method keyword)
                                   (select-keys #{:method :path})))))
                      (testing "and when sending a request to deleted simulator"
                        (let [response (test.http/get "/simulators/some/missing" content-type)]
                          (testing "returns a client error"
                            (is (test.http/client-error? response)))
                          (testing "does not publish event on activity feed"
                            (async/put! chan ::nothing)
                            (is (= ::nothing (chans/<⏰!! chan)))))))))))
            (test.ws/close! ws)
            (async/close! chan)))))))
