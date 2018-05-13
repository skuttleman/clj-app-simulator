(ns com.ben-allred.clj-app-simulator.api.services.simulators.store.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [test.utils.spies :as spies]
            [com.ben-allred.collaj.core :as collaj]
            [com.ben-allred.clj-app-simulator.api.services.simulators.store.core :as store]
            [com.ben-allred.clj-app-simulator.api.services.simulators.store.reducers :as reducers]
            [com.ben-allred.collaj.enhancers :as collaj.enhancers]))

(deftest ^:unit http-store-test
  (testing "(http-store)"
    (testing "creates a collaj store with http reducer"
      (let [spy (spies/create (constantly ::store))]
        (with-redefs [collaj/create-store spy]
          (let [store (store/http-store)]
            (is (spies/called-with? spy reducers/http))
            (is (= store ::store))))))))

(deftest ^:unit ws-store-test
  (testing "(ws-store)"
    (testing "creates a collaj store with http reducer"
      (let [spy (spies/create (constantly ::store))]
        (with-redefs [collaj/create-store spy]
          (let [store (store/ws-store)]
            (is (spies/called-with? spy reducers/ws collaj.enhancers/with-fn-dispatch))
            (is (= store ::store))))))))

(deftest ^:unit delay-test
  (testing "(delay)"
    (testing "retrieves delay from store"
      (is (= ::delay (store/delay {:config {:current {:delay ::delay}}}))))))

(deftest ^:unit response-test
  (testing "(response)"
    (testing "retrieves response from store"
      (is (= ::response (store/response {:config {:current {:response ::response}}}))))))

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
        (is (= {:requests ::requests :config ::config :sockets #{::1 ::2} :messages ::messages}
               actual))))))
