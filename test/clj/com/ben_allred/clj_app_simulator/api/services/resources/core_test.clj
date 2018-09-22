(ns com.ben-allred.clj-app-simulator.api.services.resources.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.api.services.resources.core :as resources]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.api.services.activity :as activity]
            [com.ben-allred.clj-app-simulator.utils.uuids :as uuids])
  (:import (java.util Date)))

(deftest ^:unit upload!-test
  (testing "(upload!)"
    (testing "when uploading new files"
      (let [uploads (atom {::env {111 {:filename     ::filename-1
                                       :file         ::file-1
                                       :content-type ::content-type-1
                                       :timestamp    123}}})
            publish-spy (spies/create)
            uuid-spy (spies/create)]
        (with-redefs [resources/uploads uploads
                      activity/publish publish-spy
                      uuids/random uuid-spy]
          (spies/returning! uuid-spy 222 333)
          (let [result (resources/upload! ::env [{:tempfile ::file-2 :filename ::filename-2 :content-type ::content-type-2}
                                                 {:tempfile ::file-3 :filename ::filename-3 :content-type ::content-type-3}])
                [[_ event-1 data-1] [_ event-2 data-2]] (spies/calls publish-spy)
                state (::env @uploads)]

            (testing "publishes an event for file-2"
              (is (= :resources/add event-1))
              (is (= {:filename ::filename-2 :content-type ::content-type-2 :id 222}
                     (dissoc data-1 :timestamp)))
              (is (> 50 (- (.getTime (Date.)) (.getTime (:timestamp data-1))))))

            (testing "publishes an event for file-3"
              (is (= :resources/add event-2))
              (is (= {:filename ::filename-3 :content-type ::content-type-3 :id 333}
                     (dissoc data-2 :timestamp)))
              (is (> 50 (- (.getTime (Date.)) (.getTime (:timestamp data-2))))))

            (testing "has file-1"
              (is (= ::file-1 (get-in state [111 :file]))))

            (testing "has file-2"
              (is (= ::file-2 (get-in state [222 :file]))))

            (testing "has file-3"
              (is (= ::file-3 (get-in state [333 :file]))))

            (testing "returns added files"
              (is (= (map #(dissoc % :timestamp) result)
                     [{:filename ::filename-2 :content-type ::content-type-2 :id 222}
                      {:filename ::filename-3 :content-type ::content-type-3 :id 333}])))))))

    (testing "when uploading a replacement file"
      (let [uploads (atom {::env {111 {:filename     ::filename-1
                                       :file         ::file-1
                                       :content-type ::content-type-1
                                       :timestamp    123}}})
            publish-spy (spies/create)]
        (with-redefs [resources/uploads uploads
                      activity/publish publish-spy
                      uuids/->uuid identity]
          (let [result (resources/upload! ::env 111 {:tempfile ::file-3 :filename ::filename-3 :content-type ::content-type-3})
                [_ event data] (first (spies/calls publish-spy))
                state (::env @uploads)]

            (testing "publishes an event for file-3"
              (is (= :resources/put event))
              (is (= {:filename ::filename-3 :content-type ::content-type-3 :id 111}
                     (dissoc data :timestamp)))
              (is (> 50 (- (.getTime (Date.)) (.getTime (:timestamp data))))))

            (testing "has file-3"
              (is (= ::file-3 (get-in state [111 :file]))))

            (testing "returns added file"
              (is (= (dissoc result :timestamp)
                     {:filename ::filename-3 :content-type ::content-type-3 :id 111})))))))))

(deftest ^:unit clear!-test
  (testing "(clear!)"
    (let [uploads (atom {::env {111 {:filename     ::file-3
                                     :content-type ::content-type
                                     :timestamp    789}
                                222 {:filename     ::file-1
                                     :content-type ::content-type
                                     :timestamp    123}
                                333 {:filename     ::file-2
                                     :content-type ::content-type
                                     :timestamp    456}}
                         ::other-env {444 {}}})
          publish-spy (spies/create)]
      (with-redefs [resources/uploads uploads
                    activity/publish publish-spy]
        (resources/clear! ::env)
        (testing "clears uploads"
          (is (empty? (::env @uploads)))
          (is (seq (::other-env @uploads))))

        (testing "publishes an event"
          (is (spies/called-with? publish-spy ::env :resources/clear nil)))))))

(deftest ^:unit remove!-test
  (testing "(remove!)"
    (let [upload-data {::env {111 {:filename     ::file-3
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
                                   :timestamp    456}}}
          uploads (atom upload-data)
          publish-spy (spies/create)]
      (with-redefs [uuids/->uuid identity
                    resources/uploads uploads
                    activity/publish publish-spy]
        (resources/remove! ::env 222)
        (testing "clears uploads"
          (is (= (update upload-data ::env dissoc 222) @uploads)))

        (testing "publishes an event"
          (let [[_ event data] (first (spies/calls publish-spy))]
            (is (= :resources/remove event))
            (is (-> upload-data
                    (get-in [::env 222])
                    (dissoc :file)
                    (assoc :id 222)
                    (= data)))))

        (testing "when deleting a resource that does not exist"
          (reset! uploads upload-data)
          (spies/reset! publish-spy)
          (resources/remove! ::env 999)

          (testing "does not publish an event"
            (is (= upload-data @uploads))
            (is (spies/never-called? publish-spy))))))))

(deftest ^:unit list-files-test
  (testing "(list-files)"
    (let [uploads (atom {::env {111 {:filename     ::file-3
                                     :content-type ::content-type
                                     :timestamp    789}
                                222 {:filename     ::file-1
                                     :content-type ::content-type
                                     :timestamp    123}
                                333 {:filename     ::file-2
                                     :content-type ::content-type
                                     :timestamp    456}}})]
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
                 (resources/list-files ::env))))))))

(deftest ^:unit get-test
  (testing "(get)"
    (with-redefs [uuids/->uuid identity
                  resources/uploads (atom {::env {111 {:filename     ::file
                                                       :content-type ::content-type
                                                       :timestamp    123}}})]
      (testing "returns found item"
        (is (= {:filename     ::file
                :content-type ::content-type
                :timestamp    123}
               (resources/get ::env 111))))

      (testing "returns nil when no item found"
        (is (nil? (resources/get ::env 999)))))))

(deftest ^:unit has-upload?-test
  (testing "(has-upload?)"
    (let [uuid-spy (spies/create identity)]
      (with-redefs [uuids/->uuid uuid-spy
                    resources/uploads (atom {::env {111 ::file-111}
                                             ::env-2 {222 ::file-222}})]
        (testing "returns found item"
          (is (true? (resources/has-upload? 111)))
          (is (spies/called-with? uuid-spy 111))
          (is (true? (resources/has-upload? 222)))
          (is (spies/called-with? uuid-spy 222)))

        (testing "returns nil when no item found"
          (is (false? (resources/has-upload? 999)))
          (is (spies/called-with? uuid-spy 999)))))))
