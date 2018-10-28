(ns com.ben-allred.app-simulator.api.services.simulators.store.core-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [com.ben-allred.app-simulator.api.services.resources.core :as resources]
    [com.ben-allred.app-simulator.api.services.simulators.store.core :as store]
    [com.ben-allred.app-simulator.api.services.simulators.store.reducers :as reducers]
    [com.ben-allred.app-simulator.api.services.streams :as streams]
    [com.ben-allred.collaj.core :as collaj]
    [com.ben-allred.collaj.enhancers :as collaj.enhancers]
    [test.utils.spies :as spies]))

(deftest ^:unit http-store-test
  (testing "(http-store)"
    (testing "creates a collaj store with http reducer"
      (let [spy (spies/constantly ::store)]
        (with-redefs [collaj/create-store spy]
          (let [store (store/http-store)]
            (is (spies/called-with? spy reducers/http))
            (is (= store ::store))))))))

(deftest ^:unit ws-store-test
  (testing "(ws-store)"
    (testing "creates a collaj store with http reducer"
      (let [spy (spies/constantly ::store)]
        (with-redefs [collaj/create-store spy]
          (let [store (store/ws-store)]
            (is (spies/called-with? spy reducers/ws collaj.enhancers/with-fn-dispatch))
            (is (= store ::store))))))))

(deftest ^:unit file-store-test
  (testing "(file-store)"
    (testing "creates a collaj store with http reducer"
      (let [spy (spies/constantly ::store)]
        (with-redefs [collaj/create-store spy]
          (let [store (store/file-store)]
            (is (spies/called-with? spy reducers/http))
            (is (= store ::store))))))))

(deftest ^:unit delay-test
  (testing "(delay)"
    (testing "retrieves delay from store"
      (is (= ::delay (store/delay {:config {:current {:delay ::delay}}}))))))

(deftest ^:unit response-test
  (testing "(response)"
    (testing "retrieves response from store"
      (is (= ::response (store/response {:config {:current {:response ::response}}}))))))

(deftest ^:unit file-response-test
  (testing "(file-response)"
    (let [resources-spy (spies/constantly {:content-type ::content-type
                                           :size 12345
                                           :filename "filename.ext"
                                           :file ::file})
          stream-spy (spies/constantly ::file-stream)]
      (with-redefs [resources/get resources-spy
                    streams/open-input-stream stream-spy]
        (testing "retrieves response from store as file"
          (is (= {:status ::status
                  :body ::file-stream
                  :headers {"a-header" "value"
                            "Content-Type" ::content-type
                            "Content-Length" 12345
                            "Content-Disposition" "inline; filename=\"filename.ext\""}}
                 (store/file-response ::env
                                      {:config {:current {:response {:headers {:a-header "value"}
                                                                     :file    ::123
                                                                     :status  ::status}}}})))
          (is (spies/called-with? resources-spy ::env ::123))
          (is (spies/called-with? stream-spy ::file)))))))

(deftest ^:unit requests-test
  (testing "(requests)"
    (testing "retrieves requests from store"
      (is (= ::requests (store/requests {:requests ::requests}))))))

(deftest ^:unit details-test
  (testing "(details)"
    (testing "retrieves current config and requests from store"
      (let [actual (store/details {:requests ::requests
                                   :config {:current ::config}
                                   :sockets {::1 ::ws ::2 ::ws ::3 nil}
                                   :messages ::messages})]
        (is (= {:requests ::requests :config ::config :sockets #{::1 ::2}}
               actual))))))
