(ns com.ben-allred.clj-app-simulator.api.services.resources.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.api.services.resources.core :as resources]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.api.services.activity :as activity]
            [com.ben-allred.clj-app-simulator.utils.uuids :as uuids])
  (:import (java.util Date)))

(deftest ^:unit upload!-test
  (testing "(upload!)"
    (let [uploads (atom {111 {:filename     ::filename-1
                              :file         ::file-1
                              :content-type ::content-type-1
                              :timestamp    123}})
          publish-spy (spies/create)
          uuid-spy (spies/create)]
      (with-redefs [resources/uploads uploads
                    activity/publish publish-spy
                    uuids/random uuid-spy]
        (spies/returning! uuid-spy 222 333)
        (let [result (resources/upload! [{:tempfile ::file-2 :filename ::filename-2 :content-type ::content-type-2}
                                         {:tempfile ::file-3 :filename ::filename-3 :content-type ::content-type-3}])
              [[event-1 data-1] [event-2 data-2]] (spies/calls publish-spy)
              data @uploads]

          (testing "publishes an event for file-2"
            (is (= :files.upload/receive event-1))
            (is (= {:filename ::filename-2 :content-type ::content-type-2 :id 222}
                   (dissoc data-1 :timestamp)))
            (is (> 50 (- (.getTime (Date.)) (.getTime (:timestamp data-1))))))

          (testing "publishes an event for file-3"
            (is (= :files.upload/receive event-2))
            (is (= {:filename ::filename-3 :content-type ::content-type-3 :id 333}
                   (dissoc data-2 :timestamp)))
            (is (> 50 (- (.getTime (Date.)) (.getTime (:timestamp data-2))))))

          (testing "has file-1"
            (is (= ::file-1 (get-in data [111 :file]))))

          (testing "has file-2"
            (is (= ::file-2 (get-in data [222 :file]))))

          (testing "has file-3"
            (is (= ::file-3 (get-in data [333 :file]))))

          (testing "returns added files"
            (is (= (map #(dissoc % :timestamp) result)
                   [{:filename ::filename-2 :content-type ::content-type-2 :id 222}
                    {:filename ::filename-3 :content-type ::content-type-3 :id 333}]))))))))

(deftest ^:unit clear!-test
  (testing "(clear!)"
    (let [uploads (atom {111 {:filename     ::file-3
                              :content-type ::content-type
                              :timestamp    789}
                         222 {:filename     ::file-1
                              :content-type ::content-type
                              :timestamp    123}
                         333 {:filename     ::file-2
                              :content-type ::content-type
                              :timestamp    456}})
          publish-spy (spies/create)]
      (with-redefs [resources/uploads uploads
                    activity/publish publish-spy]
        (resources/clear!)
        (testing "clears uploads"
          (is (empty? @uploads)))

        (testing "publishes an event"
          (is (spies/called-with? publish-spy :files/clear nil)))))))

(deftest ^:unit remove!-test
  (testing "(remove!)"
    (let [upload-data {111 {:filename     ::file-3
                            :content-type ::content-type
                            :file         ::file
                            :timestamp    789}
                       222 {:filename     ::file-1
                            :content-type ::content-type
                            :file         ::file
                            :timestamp    123}
                       333 {:filename     ::file-2
                            :content-type ::content-type
                            :file         ::file
                            :timestamp    456}}
          uploads (atom upload-data)
          publish-spy (spies/create)]
      (with-redefs [uuids/->uuid identity
                    resources/uploads uploads
                    activity/publish publish-spy]
        (resources/remove! 222)
        (testing "clears uploads"
          (is (= (dissoc upload-data 222) @uploads)))

        (testing "publishes an event"
          (let [[event data] (first (spies/calls publish-spy))]
            (is (= :files/remove event))
            (is (-> upload-data
                    (get 222)
                    (dissoc :file)
                    (assoc :id 222)
                    (= data)))))

        (testing "when deleting a resource that does not exist"
          (reset! uploads upload-data)
          (spies/reset! publish-spy)
          (resources/remove! 999)

          (testing "does not publish an event"
            (is (= upload-data @uploads))
            (is (spies/never-called? publish-spy))))))))

(deftest ^:unit list-files-test
  (testing "(list-files)"
    (let [uploads (atom {111 {:filename     ::file-3
                              :content-type ::content-type
                              :timestamp    789}
                         222 {:filename     ::file-1
                              :content-type ::content-type
                              :timestamp    123}
                         333 {:filename     ::file-2
                              :content-type ::content-type
                              :timestamp    456}})]
      (with-redefs [resources/uploads uploads]
        (testing "returns a list of files"
          (is (= [{:id           222
                   :filename     ::file-1
                   :content-type ::content-type
                   :timestamp    123}
                  {:id           333
                   :filename     ::file-2
                   :content-type ::content-type
                   :timestamp    456}
                  {:id           111
                   :filename     ::file-3
                   :content-type ::content-type
                   :timestamp    789}]
                 (resources/list-files))))))))

(deftest ^:unit get-test
  (testing "(get)"
    (with-redefs [uuids/->uuid identity
                  resources/uploads (atom {111 {:filename     ::file
                                                :content-type ::content-type
                                                :timestamp    123}})]
      (testing "returns found item"
        (is (= {:filename     ::file
                :content-type ::content-type
                :timestamp    123}
               (resources/get 111))))

      (testing "returns nil when no item found"
        (is (nil? (resources/get 999)))))))

(deftest ^:unit has-upload?-test
  (testing "(has-upload?)"
    (with-redefs [uuids/->uuid identity
                  resources/uploads (atom {111 {:filename     ::file
                                                :content-type ::content-type
                                                :timestamp    123}})]
      (testing "returns found item"
        (is (true? (resources/has-upload? 111))))

      (testing "returns nil when no item found"
        (is (false? (resources/has-upload? 999)))))))
