(ns com.ben-allred.app-simulator.api.services.resources.core-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [com.ben-allred.app-simulator.api.services.activity :as activity]
    [com.ben-allred.app-simulator.api.services.resources.core :as resources]
    [com.ben-allred.app-simulator.api.services.streams :as streams]
    [com.ben-allred.app-simulator.api.utils.specs :as specs]
    [com.ben-allred.app-simulator.utils.uuids :as uuids]
    [test.utils.spies :as spies])
  (:import
    (clojure.lang ExceptionInfo)
    (java.util Date)))

(deftest ^:unit upload!-test
  (testing "(upload!)"
    (with-redefs [specs/valid? (constantly false)]
      (testing "when uploading bad files"
        (is (thrown? ExceptionInfo (resources/upload! ::env [::bad-file])))
        (is (thrown? ExceptionInfo (resources/upload! ::env (uuids/random) [::bad-file])))))

    (testing "when uploading new files"
      (let [uploads (atom {::env {111 {:filename     ::filename-1
                                       :file         ::file-1
                                       :content-type ::content-type-1
                                       :timestamp    123}}})]
        (with-redefs [specs/valid? (constantly true)
                      resources/uploads uploads
                      activity/publish (spies/create)
                      uuids/random (spies/create)
                      streams/file? (constantly true)]
          (spies/returning! uuids/random 222 333)
          (let [result (resources/upload! ::env [{:tempfile ::file-2 :filename ::filename-2 :content-type ::content-type-2}
                                                 {:tempfile ::file-3 :filename ::filename-3 :content-type ::content-type-3}])
                [[_ event-1 data-1] [_ event-2 data-2]] (spies/calls activity/publish)
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
                                       :tempfile     ::temp-1
                                       :content-type ::content-type-1
                                       :timestamp    123}}})]
        (with-redefs [specs/valid? (constantly true)
                      resources/uploads uploads
                      activity/publish (spies/create)
                      uuids/->uuid identity
                      streams/delete (spies/create)
                      streams/file? (constantly true)]
          (let [result (resources/upload! ::env 111 {:tempfile ::file-3 :filename ::filename-3 :content-type ::content-type-3})
                [_ event data] (first (spies/calls activity/publish))
                state (::env @uploads)]

            (testing "publishes an event for file-3"
              (is (= :resources/put event))
              (is (= {:filename ::filename-3 :content-type ::content-type-3 :id 111}
                     (dissoc (:resource data) :timestamp)))
              (is (> 50 (- (.getTime (Date.)) (.getTime (get-in data [:resource :timestamp]))))))

            (testing "has file-3"
              (is (= ::file-3 (get-in state [111 :file]))))

            (testing "deletes the existing file"
              (is (spies/called-with? streams/delete ::temp-1)))

            (testing "returns added file"
              (is (= (dissoc result :timestamp)
                     {:filename ::filename-3 :content-type ::content-type-3 :id 111})))))))))

(deftest ^:unit clear!-test
  (testing "(clear!)"
    (let [uploads (atom {::env       {111 {:filename     ::file-3
                                           :content-type ::content-type
                                           :timestamp    789
                                           :tempfile     ::temp-111
                                           :file         ::file-111}
                                      222 {:filename     ::file-1
                                           :content-type ::content-type
                                           :timestamp    123
                                           :file         ::file-222}
                                      333 {:filename     ::file-2
                                           :content-type ::content-type
                                           :tempfile     ::temp-333
                                           :timestamp    456
                                           :file         ::file-333}}
                         ::other-env {444 {}}})]
      (with-redefs [resources/uploads uploads
                    activity/publish (spies/create)
                    streams/delete (spies/create)]
        (resources/clear! ::env)
        (testing "clears uploads"
          (is (empty? (::env @uploads)))
          (is (seq (::other-env @uploads)))
          (is (spies/called-times? streams/delete 3))
          (is (spies/called-with? streams/delete ::temp-111))
          (is (spies/called-with? streams/delete ::temp-333)))

        (testing "publishes an event"
          (is (spies/called-with? activity/publish ::env :resources/clear nil)))))))

(deftest ^:unit remove!-test
  (testing "(remove!)"
    (let [upload-data {::env {111 {:filename     ::file-3
                                   :content-type ::content-type
                                   :file         ::file-111
                                   :timestamp    789}
                              222 {:filename     ::file-1
                                   :content-type ::content-type
                                   :file         ::file-222
                                   :tempfile     ::file-222
                                   :timestamp    123}
                              333 {:filename     ::file-2
                                   :content-type ::content-type
                                   :file         ::file-333
                                   :timestamp    456}}}
          uploads (atom upload-data)]
      (with-redefs [uuids/->uuid identity
                    resources/uploads uploads
                    activity/publish (spies/create)
                    streams/delete (spies/create)]
        (resources/remove! ::env 222)
        (testing "clears uploads"
          (is (= (dissoc (::env upload-data) 222) (::env @uploads)))
          (is (spies/called-with? streams/delete ::file-222)))

        (testing "publishes an event"
          (let [[_ event data] (first (spies/calls activity/publish))]
            (is (= :resources/remove event))
            (is (-> upload-data
                    (get-in [::env 222])
                    (dissoc :file :tempfile)
                    (= (:resource data))))))

        (testing "when deleting a resource that does not exist"
          (reset! uploads upload-data)
          (spies/reset! activity/publish)
          (resources/remove! ::env 999)

          (testing "does not publish an event"
            (is (= upload-data @uploads))
            (is (spies/never-called? activity/publish))))

        (testing "when deleting a resource that has no tempfile"
          (reset! uploads upload-data)
          (spies/reset! streams/delete)
          (resources/remove! ::env 333)
          (testing "clears uploads"
            (is (= (dissoc (::env upload-data) 333) (::env @uploads)))
            (is (->> (spies/calls streams/delete)
                     (remove (partial every? nil?))
                     (empty?))))

          (testing "publishes an event"
            (let [[_ event data] (first (spies/calls activity/publish))]
              (is (= :resources/remove event))
              (is (-> upload-data
                      (get-in [::env 333])
                      (dissoc :file)
                      (= (:resource data)))))))))))

(deftest ^:unit list-files-test
  (testing "(list-files)"
    (with-redefs [resources/uploads (atom {::env {111 {:filename     ::file-3
                                                       :content-type ::content-type
                                                       :timestamp    789}
                                                  222 {:filename     ::file-1
                                                       :content-type ::content-type
                                                       :timestamp    123}
                                                  333 {:filename     ::file-2
                                                       :content-type ::content-type
                                                       :timestamp    456}}})]
      (testing "returns a list of files"
        (is (= [{:filename     ::file-1
                 :content-type ::content-type
                 :timestamp    123}
                {:filename     ::file-2
                 :content-type ::content-type
                 :timestamp    456}
                {:filename     ::file-3
                 :content-type ::content-type
                 :timestamp    789}]
               (resources/list-files ::env)))))))

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
    (with-redefs [uuids/->uuid (spies/create identity)
                  resources/uploads (atom {::env   {111 ::file-111}
                                           ::env-2 {222 ::file-222}})]
      (testing "returns found item"
        (is (true? (resources/has-upload? 111)))
        (is (spies/called-with? uuids/->uuid 111))
        (is (true? (resources/has-upload? 222)))
        (is (spies/called-with? uuids/->uuid 222)))

      (testing "returns false when no item found"
        (is (false? (resources/has-upload? 999)))))))
