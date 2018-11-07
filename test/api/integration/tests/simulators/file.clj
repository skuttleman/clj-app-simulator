(ns integration.tests.simulators.file
  (:require
    [clojure.core.async :as async]
    [clojure.edn :as edn]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [com.ben-allred.app-simulator.core :as sim-core]
    [com.ben-allred.app-simulator.utils.json :as json]
    [com.ben-allred.app-simulator.utils.logging :as log]
    [com.ben-allred.app-simulator.utils.transit :as transit]
    [com.ben-allred.app-simulator.utils.uuids :as uuids]
    [integration.utils.api :as test.api]
    [integration.utils.chans :as chans]
    [integration.utils.fixtures :as fixtures]
    [integration.utils.http :as test.http]
    [integration.utils.ws :as test.ws]))

(use-fixtures :once fixtures/run-server)

(def ^:private content-type->parser
  {"application/edn"     edn/read-string
   "application/transit" transit/parse
   "application/json"    json/parse})

(deftest ^:integration resources-web-test
  (testing "[resources Web API]"
    (doseq [content-type ["application/edn" "application/transit" "application/json"]]
      (test.http/delete "/api/resources" content-type)
      (let [chan (async/chan 64)
            ws (test.ws/connect "/api/simulators/activity"
                                :query-params {:accept content-type}
                                :on-msg (partial async/put! chan)
                                :to-clj (content-type->parser content-type))]
        (testing "when uploading a resource"
          (let [response (test.http/upload "/api/resources" content-type "sample.txt")]
            (is (test.http/success? response)))

          (let [{:keys [event] {{id-1 :id filename-1 :filename} :resource} :data} (chans/<⏰!! chan)]
            (testing "publishes an event"
              (is (= :resources/put (keyword event)))
              (is (= "sample.txt" filename-1)))

            (testing "and when getting a list of resources"
              (let [[_ body :as response] (test.http/get "/api/resources" content-type)
                    results (set (map (juxt :id :filename) (:resources body)))]
                (testing "includes the uploaded resource"
                  (is (test.http/success? response))
                  (is (contains? results [id-1 "sample.txt"])))

                (testing "and when uploading a second resource"
                  (test.http/upload "/api/resources" content-type "sample2.txt")

                  (let [{:keys [event] {{id-2 :id filename-2 :filename} :resource} :data} (chans/<⏰!! chan)]
                    (testing "publishes an event"
                      (is (= :resources/put (keyword event)))
                      (is (= "sample2.txt" filename-2)))

                    (testing "and when getting a list of resources"
                      (let [[_ body :as response] (test.http/get "/api/resources" content-type)
                            results (set (map (juxt :id :filename) (:resources body)))]
                        (testing "includes both resources"
                          (is (test.http/success? response))
                          (is (contains? results [id-1 "sample.txt"]))
                          (is (contains? results [id-2 "sample2.txt"])))

                        (testing "and when deleting all resources"
                          (is (test.http/success? (test.http/delete "/api/resources" content-type)))

                          (let [{:keys [event]} (chans/<⏰!! chan)]
                            (testing "publishes an event"
                              (is (= :resources/clear (keyword event)))))

                          (testing "and when getting a list of resources"
                            (let [[_ body :as response] (test.http/get "/api/resources" content-type)]
                              (testing "returns an empty list"
                                (is (test.http/success? response))
                                (is (empty? (:resources body)))))))))))))))

        (testing "when uploading two resources"
          (test.http/upload "/api/resources" content-type "sample.txt" "sample2.txt")

          (let [{event-1 :event {data-1 :resource} :data} (chans/<⏰!! chan)
                {event-2 :event {data-2 :resource} :data} (chans/<⏰!! chan)
                id-1 (:id (first (filter (comp #{"sample.txt"} :filename) [data-1 data-2])))
                id-2 (:id (first (filter (comp #{"sample2.txt"} :filename) [data-1 data-2])))]

            (testing "publishes an event for each file"
              (is (= :resources/put (keyword event-1)))
              (is (= :resources/put (keyword event-2)))
              (is (and id-1 id-2)))

            (testing "and when getting a list of resources"
              (let [[_ body :as response] (test.http/get "/api/resources" content-type)
                    results (set (map (juxt :id :filename) (:resources body)))]
                (testing "returns both resources"
                  (is (test.http/success? response))
                  (is (contains? results [id-1 "sample.txt"]))
                  (is (contains? results [id-2 "sample2.txt"])))

                (testing "and when deleting one resource"
                  (test.http/delete (str "/api/resources/" id-1) content-type)

                  (let [{:keys [event data]} (chans/<⏰!! chan)]
                    (testing "publishes an event"
                      (is (= :resources/remove (keyword event)))
                      (is (= {:filename "sample.txt" :id id-1} (select-keys (:resource data) #{:filename :id}))))

                    (testing "and when getting a list of resources"
                      (let [[_ body :as response] (test.http/get "/api/resources" content-type)
                            results (set (map (juxt :id :filename) (:resources body)))]
                        (is (test.http/success? response))

                        (testing "includes the resource that was not deleted"
                          (is (contains? results [id-2 "sample2.txt"])))

                        (testing "does not include the deleted resource"
                          (is (not (contains? results [id-1 "sample.txt"]))))))))))))

        (test.ws/close! ws)
        (async/close! chan)))))

(deftest ^:integration resources-api-test
  (testing "[resources Clojure API]"
    (doseq [content-type ["application/edn" "application/transit" "application/json"]]
      (sim-core/delete-resources!)
      (let [chan (async/chan 64)
            ws (test.ws/connect "/api/simulators/activity"
                                :query-params {:accept content-type}
                                :on-msg (partial async/put! chan)
                                :to-clj (content-type->parser content-type))]
        (testing "when uploading a resource"
          (sim-core/save-resources [(test.api/fixture->file "sample.txt")])

          (let [{:keys [event] {{id-1 :id filename-1 :filename} :resource} :data} (chans/<⏰!! chan)]
            (testing "publishes an event"
              (is (= :resources/put (keyword event)))
              (is (= "sample.txt" filename-1)))

            (testing "and when getting a list of resources"
              (let [files (sim-core/list-resources)
                    results (set (map (juxt :id :filename) files))]
                (testing "includes the uploaded resource"
                  (is (contains? results [(uuids/->uuid id-1) "sample.txt"])))

                (testing "and when uploading a second resource"
                  (sim-core/save-resources [(test.api/fixture->file "sample2.txt")])

                  (let [{:keys [event] {{id-2 :id filename-2 :filename} :resource} :data} (chans/<⏰!! chan)]
                    (testing "publishes an event"
                      (is (= :resources/put (keyword event)))
                      (is (= "sample2.txt" filename-2)))

                    (testing "and when getting a list of resources"
                      (let [files (sim-core/list-resources)
                            results (set (map (juxt :id :filename) files))]
                        (testing "includes both resources"
                          (is (contains? results [(uuids/->uuid id-1) "sample.txt"]))
                          (is (contains? results [(uuids/->uuid id-2) "sample2.txt"])))

                        (testing "and when deleting all resources"
                          (sim-core/delete-resources!)

                          (let [{:keys [event]} (chans/<⏰!! chan)]
                            (testing "publishes an event"
                              (is (= :resources/clear (keyword event)))))

                          (testing "and when getting a list of resources"
                            (let [files (sim-core/list-resources)]
                              (testing "returns an empty list"
                                (is (empty? files))))))))))))))

        (testing "when uploading two resources"
          (sim-core/save-resources (map test.api/fixture->file ["sample.txt" "sample2.txt"]))

          (let [{event-1 :event {data-1 :resource} :data} (chans/<⏰!! chan)
                {event-2 :event {data-2 :resource} :data} (chans/<⏰!! chan)
                id-1 (uuids/->uuid (:id (first (filter (comp #{"sample.txt"} :filename) [data-1 data-2]))))
                id-2 (uuids/->uuid (:id (first (filter (comp #{"sample2.txt"} :filename) [data-1 data-2]))))]

            (testing "publishes an event for each file"
              (is (= :resources/put (keyword event-1)))
              (is (= :resources/put (keyword event-2)))
              (is (and id-1 id-2)))

            (testing "and when getting a list of resources"
              (let [files (sim-core/list-resources)
                    results (set (map (juxt :id :filename) files))]
                (testing "returns both resources"
                  (is (contains? results [id-1 "sample.txt"]))
                  (is (contains? results [id-2 "sample2.txt"])))

                (testing "and when deleting one resource"
                  (sim-core/delete-resources! id-1)

                  (let [{:keys [event data]} (chans/<⏰!! chan)]
                    (testing "publishes an event"
                      (is (= :resources/remove (keyword event)))
                      (is (= {:filename "sample.txt" :id id-1} (-> data
                                                                   (:resource)
                                                                   (select-keys #{:filename :id})
                                                                   (update :id uuids/->uuid)))))

                    (testing "and when getting a list of resources"
                      (let [files (sim-core/list-resources)
                            results (set (map (juxt :id :filename) files))]
                        (testing "includes the resource that was not deleted"
                          (is (contains? results [id-2 "sample2.txt"])))

                        (testing "does not include the deleted resource"
                          (is (not (contains? results [id-1 "sample.txt"]))))))))))))

        (test.ws/close! ws)
        (async/close! chan)))))

(deftest ^:integration file-simulator-web-test
  (testing "[simulator Web API]"
    (doseq [content-type ["application/edn" "application/transit" "application/json"]]
      (test.http/delete "/api/resources" content-type)
      (let [chan (async/chan 64)
            ws (test.ws/connect "/api/simulators/activity"
                                :query-params {:accept content-type}
                                :on-msg (partial async/put! chan)
                                :to-clj (content-type->parser content-type))]
        (testing "when saving two resources"
          (test.http/upload "/api/resources" content-type "sample.txt" "sample2.txt")
          (let [{{data-1 :resource} :data} (chans/<⏰!! chan)
                {{data-2 :resource} :data} (chans/<⏰!! chan)
                id-1 (:id (first (filter (comp #{"sample.txt"} :filename) [data-1 data-2])))
                id-2 (:id (first (filter (comp #{"sample2.txt"} :filename) [data-1 data-2])))]
            (testing "publishes an event for each file"
              (is (and id-1 id-2)))
            (testing (str "and when saving initial simulators with content-type: " (pr-str content-type))
              (let [[_ body :as response] (test.http/post "/api/simulators/init"
                                                          content-type
                                                          {:body {:simulators [{:method   :file/get
                                                                                :path     "/some/file"
                                                                                :response {:status 202 :file id-1}}]}})
                    simulator-id (:id (first (get-in body [:simulators])))]
                (testing "publishes an event"
                  (let [{:keys [event]} (chans/<⏰!! chan)]
                    (is (= :simulators/init (keyword event)))))

                (testing "returns a success response"
                  (is (test.http/success? response))
                  (is (= id-1 (get-in (first (:simulators body)) [:config :response :file]))))

                (testing "and when making a request to a file simulator"
                  (let [[_ body :as response] (test.http/get "/simulators/some/file?some=qp" "text/plain")]
                    (testing "returns the file data"
                      (is (test.http/success? response))
                      (is (= body (test.api/slurp "sample.txt")))))

                  (testing "publishes an event"
                    (let [{:keys [event data]} (chans/<⏰!! chan)]
                      (is (= :simulators/receive (keyword event)))
                      (is (= {:some "qp"} (get-in data [:request :query-params])))))

                  (testing "and when updating the file-id for the simulator"
                    (let [response (test.http/patch (str "/api/simulators/" simulator-id)
                                                    content-type
                                                    {:body {:action :simulators/change
                                                            :config {:response {:file id-2}}}})]
                      (is (test.http/success? response)))

                    (testing "publishes an event"
                      (let [{:keys [event data]} (chans/<⏰!! chan)]
                        (is (= :simulators/change (keyword event)))
                        (is (= id-2 (get-in data [:simulator :config :response :file])))))

                    (testing "and when making a request to the file simulator"
                      (let [[_ body :as response] (test.http/get "/simulators/some/file?some=new-qp" "text/plain")]
                        (testing "returns the new file data"
                          (is (test.http/success? response))
                          (is (= body (test.api/slurp "sample2.txt")))))

                      (testing "publishes an event"
                        (let [{:keys [event data]} (chans/<⏰!! chan)]
                          (is (= :simulators/receive (keyword event)))
                          (is (= {:some "new-qp"} (get-in data [:request :query-params])))))

                      (testing "and when updating the resource"
                        (test.http/upload-put (str "/api/resources/" id-2) content-type "sample3.txt")

                        (testing "publishes an event"
                          (let [{{{:keys [id filename]} :resource} :data event :event} (chans/<⏰!! chan)]
                            (is (= id id-2))
                            (is (= "sample3.txt" filename))
                            (is (= :resources/put (keyword event))))

                          (testing "and when making a request to the file simulator"
                            (let [[_ body :as response] (test.http/get "/simulators/some/file" "text/plain")]
                              (testing "returns the new file data"
                                (is (test.http/success? response))
                                (is (= body (test.api/slurp "sample3.txt"))))

                              (testing "publishes an event"
                                (let [{:keys [event data]} (chans/<⏰!! chan)]
                                  (is (= :simulators/receive (keyword event)))
                                  (is (empty? (get-in data [:request :query-params])))))))))

                      (testing "and when deleting the resource"
                        (test.http/delete (str "/api/resources/" id-2) content-type)

                        (let [{:keys [event data]} (chans/<⏰!! chan)]
                          (testing "publishes an event"
                            (is (= :resources/remove (keyword event)))
                            (is (= {:filename "sample3.txt" :id id-2} (select-keys (:resource data) #{:filename :id}))))

                          (testing "and when making a request to the file simulator"
                            (let [[_ _ status] (test.http/get "/simulators/some/file" "text/plain")]

                              (testing "publishes an event"
                                (let [{:keys [event]} (chans/<⏰!! chan)]
                                  (is (= :simulators/receive (keyword event)))))

                              (testing "returns not-found"
                                (is (= status :http.status/not-found))))))))))))))

        (test.ws/close! ws)
        (async/close! chan)))))

(deftest ^:integration file-simulator-api-test
  (testing "[simulator Clojure API]"
    (doseq [content-type ["application/edn" "application/transit" "application/json"]]
      (sim-core/delete-resources!)
      (let [chan (async/chan 64)
            ws (test.ws/connect "/api/simulators/activity"
                                :query-params {:accept content-type}
                                :on-msg (partial async/put! chan)
                                :to-clj (content-type->parser content-type))]
        (testing "when saving two resources"
          (sim-core/save-resources (map test.api/fixture->file ["sample.txt" "sample2.txt"]))
          (let [{{data-1 :resource} :data} (chans/<⏰!! chan)
                {{data-2 :resource} :data} (chans/<⏰!! chan)
                id-1 (uuids/->uuid (:id (first (filter (comp #{"sample.txt"} :filename) [data-1 data-2]))))
                id-2 (uuids/->uuid (:id (first (filter (comp #{"sample2.txt"} :filename) [data-1 data-2]))))]
            (testing "publishes an event for each file"
              (is (and id-1 id-2)))
            (testing (str "and when saving initial simulators with content-type: " (pr-str content-type))
              (let [files (sim-core/init-simulators [{:method   :file/get
                                                      :path     "/some/file"
                                                      :response {:status 202 :file id-1}}])
                    simulator-id (:id (first files))]
                (testing "publishes an event"
                  (let [{:keys [event]} (chans/<⏰!! chan)]
                    (is (= :simulators/init (keyword event)))))

                (testing "returns the simulator"
                  (is (= id-1 (get-in (first files) [:config :response :file]))))

                (testing "and when making a request to a file simulator"
                  (let [[_ body :as response] (test.http/get "/simulators/some/file?some=qp" "text/plain")]
                    (testing "returns the file data"
                      (is (test.http/success? response))
                      (is (= body (test.api/slurp "sample.txt")))))

                  (testing "publishes an event"
                    (let [{:keys [event data]} (chans/<⏰!! chan)]
                      (is (= :simulators/receive (keyword event)))
                      (is (= {:some "qp"} (get-in data [:request :query-params])))))

                  (testing "and when updating the file-id for the simulator"
                    (sim-core/change! simulator-id {:response {:file id-2}})

                    (testing "publishes an event"
                      (let [{:keys [event data]} (chans/<⏰!! chan)]
                        (is (= :simulators/change (keyword event)))
                        (is (= id-2 (uuids/->uuid (get-in data [:simulator :config :response :file]))))))

                    (testing "and when making a request to the file simulator"
                      (let [[_ body :as response] (test.http/get "/simulators/some/file?some=new-qp" "text/plain")]
                        (testing "returns the new file data"
                          (is (test.http/success? response))
                          (is (= body (test.api/slurp "sample2.txt")))))

                      (testing "publishes an event"
                        (let [{:keys [event data]} (chans/<⏰!! chan)]
                          (is (= :simulators/receive (keyword event)))
                          (is (= {:some "new-qp"} (get-in data [:request :query-params])))))

                      (testing "and when updating the resource"
                        (sim-core/put-resource id-2 (test.api/fixture->file "sample3.txt"))

                        (testing "publishes an event"
                          (let [{{{:keys [id filename]} :resource} :data event :event} (chans/<⏰!! chan)]
                            (is (= (uuids/->uuid id) id-2))
                            (is (= "sample3.txt" filename))
                            (is (= :resources/put (keyword event))))

                          (testing "and when making a request to the file simulator"
                            (let [[_ body :as response] (test.http/get "/simulators/some/file" "text/plain")]
                              (testing "returns the new file data"
                                (is (test.http/success? response))
                                (is (= body (test.api/slurp "sample3.txt"))))

                              (testing "publishes an event"
                                (let [{:keys [event data]} (chans/<⏰!! chan)]
                                  (is (= :simulators/receive (keyword event)))
                                  (is (empty? (get-in data [:request :query-params])))))))))

                      (testing "and when deleting the resource"
                        (sim-core/delete-resources! id-2)

                        (let [{:keys [event data]} (chans/<⏰!! chan)]
                          (testing "publishes an event"
                            (is (= :resources/remove (keyword event)))
                            (is (= {:filename "sample3.txt" :id id-2} (-> data
                                                                          (:resource)
                                                                          (select-keys #{:filename :id})
                                                                          (update :id uuids/->uuid)))))

                          (testing "and when making a request to the file simulator"
                            (let [[_ _ status] (test.http/get "/simulators/some/file" "text/plain")]

                              (testing "publishes an event"
                                (let [{:keys [event]} (chans/<⏰!! chan)]
                                  (is (= :simulators/receive (keyword event)))))

                              (testing "returns not-found"
                                (is (= status :http.status/not-found))))))))))))))

        (test.ws/close! ws)
        (async/close! chan)))))
