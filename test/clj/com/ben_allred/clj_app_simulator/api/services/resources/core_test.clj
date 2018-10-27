(ns com.ben-allred.clj-app-simulator.api.services.resources.core-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [com.ben-allred.clj-app-simulator.api.services.activity :as activity]
    [com.ben-allred.clj-app-simulator.api.services.resources.core :as resources]
    [com.ben-allred.clj-app-simulator.api.services.streams :as streams]
    [com.ben-allred.clj-app-simulator.utils.uuids :as uuids]
    [test.utils.spies :as spies])
  (:import
    (java.util Date)))

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
              (is (= :resources/put event-1))
              (is (= {:filename ::filename-2 :content-type ::content-type-2 :id 222}
                     (dissoc (:resource data-1) :timestamp)))
              (is (> 50 (- (.getTime (Date.)) (.getTime (get-in data-1 [:resource :timestamp]))))))

            (testing "publishes an event for file-3"
              (is (= :resources/put event-2))
              (is (= {:filename ::filename-3 :content-type ::content-type-3 :id 333}
                     (dissoc (:resource data-2) :timestamp)))
              (is (> 50 (- (.getTime (Date.)) (.getTime (get-in data-2 [:resource :timestamp]))))))

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
            publish-spy (spies/create)
            delete-spy (spies/create)]
        (with-redefs [resources/uploads uploads
                      activity/publish publish-spy
                      uuids/->uuid identity
                      streams/delete delete-spy]
          (let [result (resources/upload! ::env 111 {:tempfile ::file-3 :filename ::filename-3 :content-type ::content-type-3})
                [_ event data] (first (spies/calls publish-spy))
                state (::env @uploads)]

            (testing "publishes an event for file-3"
              (is (= :resources/put event))
              (is (= {:filename ::filename-3 :content-type ::content-type-3 :id 111}
                     (dissoc (:resource data) :timestamp)))
              (is (> 50 (- (.getTime (Date.)) (.getTime (get-in data [:resource :timestamp]))))))

            (testing "has file-3"
              (is (= ::file-3 (get-in state [111 :file]))))

            (testing "deletes the existing file"
              (is (spies/called-with? delete-spy ::file-1)))

            (testing "returns added file"
              (is (= (dissoc result :timestamp)
                     {:filename ::filename-3 :content-type ::content-type-3 :id 111})))))))))

(deftest ^:unit clear!-test
  (testing "(clear!)"
    (let [uploads (atom {::env       {111 {:filename     ::file-3
                                           :content-type ::content-type
                                           :timestamp    789
                                           :file         ::file-111}
                                      222 {:filename     ::file-1
                                           :content-type ::content-type
                                           :timestamp    123
                                           :file         ::file-222}
                                      333 {:filename     ::file-2
                                           :content-type ::content-type
                                           :timestamp    456
                                           :file         ::file-333}}
                         ::other-env {444 {}}})
          publish-spy (spies/create)
          delete-spy (spies/create)]
      (with-redefs [resources/uploads uploads
                    activity/publish publish-spy
                    streams/delete delete-spy]
        (resources/clear! ::env)
        (testing "clears uploads"
          (is (empty? (::env @uploads)))
          (is (seq (::other-env @uploads)))
          (is (spies/called-times? delete-spy 3))
          (is (spies/called-with? delete-spy ::file-111))
          (is (spies/called-with? delete-spy ::file-222))
          (is (spies/called-with? delete-spy ::file-333)))

        (testing "publishes an event"
          (is (spies/called-with? publish-spy ::env :resources/clear nil)))))))

(deftest ^:unit remove!-test
  (testing "(remove!)"
    (let [upload-data {::env {111 {:filename     ::file-3
                                   :content-type ::content-type
                                   :file         ::file-111
                                   :timestamp    789}
                              222 {:filename     ::file-1
                                   :content-type ::content-type
                                   :file         ::file-222
                                   :timestamp    123}
                              333 {:filename     ::file-2
                                   :content-type ::content-type
                                   :file         ::file-333
                                   :timestamp    456}}}
          uploads (atom upload-data)
          publish-spy (spies/create)
          delete-spy (spies/create)]
      (with-redefs [uuids/->uuid identity
                    resources/uploads uploads
                    activity/publish publish-spy
                    streams/delete delete-spy]
        (resources/remove! ::env 222)
        (testing "clears uploads"
          (is (= (update upload-data ::env dissoc 222) @uploads))
          (is (spies/called-with? delete-spy ::file-222)))

        (testing "publishes an event"
          (let [[_ event data] (first (spies/calls publish-spy))]
            (is (= :resources/remove event))
            (is (-> upload-data
                    (get-in [::env 222])
                    (dissoc :file)
                    (assoc :id 222)
                    (= (:resource data))))))

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
                    resources/uploads (atom {::env   {111 ::file-111}
                                             ::env-2 {222 ::file-222}})]
        (testing "returns found item"
          (is (true? (resources/has-upload? 111)))
          (is (spies/called-with? uuid-spy 111))
          (is (true? (resources/has-upload? 222)))
          (is (spies/called-with? uuid-spy 222)))

        (testing "returns nil when no item found"
          (is (false? (resources/has-upload? 999)))
          (is (spies/called-with? uuid-spy 999)))))))
