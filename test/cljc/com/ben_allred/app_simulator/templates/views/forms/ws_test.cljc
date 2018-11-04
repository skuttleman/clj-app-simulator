(ns com.ben-allred.app-simulator.templates.views.forms.ws-test
  (:require
    #?@(:cljs [[com.ben-allred.app-simulator.ui.services.forms.core :as forms]
               [com.ben-allred.app-simulator.ui.simulators.shared.interactions :as shared.interactions]
               [com.ben-allred.app-simulator.ui.simulators.ws.interactions :as interactions]])
    [clojure.test :as t :refer [deftest is testing]]
    [com.ben-allred.app-simulator.services.navigation :as nav*]
    [com.ben-allred.app-simulator.templates.fields :as fields]
    [com.ben-allred.app-simulator.templates.resources.ws :as resources]
    [com.ben-allred.app-simulator.templates.transformations.ws :as tr]
    [com.ben-allred.app-simulator.templates.views.forms.shared :as shared.views]
    [com.ben-allred.app-simulator.templates.views.forms.ws :as ws.views]
    [com.ben-allred.app-simulator.templates.views.simulators :as views.sim]
    [com.ben-allred.app-simulator.utils.dates :as dates]
    [test.utils.dom :as test.dom]
    [test.utils.spies :as spies]))

(deftest ^:unit name-field-test
  (testing "(name-field)"
    (with-redefs [shared.views/with-attrs (spies/create (fn [attrs & _] attrs))]
      (testing "when a value is supplied for auto-focus?"
        (testing "renders the field with auto-focus"
          (let [[component attrs] (ws.views/name-field ::form ::auto-focus)]
            (is (spies/called-with? shared.views/with-attrs
                                    (spies/matcher map?)
                                    ::form
                                    [:name]
                                    tr/model->view
                                    tr/view->model))
            (is (= fields/input component))
            (is (= "Name" (:label attrs)))
            (is (= ::auto-focus (:auto-focus? attrs))))))

      (testing "when no value is supplied for auto-focus?"
        (testing "defaults auto-focus to false"
          (let [[component attrs] (ws.views/name-field ::form)]
            (is (spies/called-with? shared.views/with-attrs
                                    (spies/matcher map?)
                                    ::form
                                    [:name]
                                    tr/model->view
                                    tr/view->model))
            (is (= fields/input component))
            (is (= "Name" (:label attrs)))
            (is (false? (:auto-focus? attrs)))))))))

(deftest ^:unit group-field-test
  (testing "(group-field)"
    (testing "wraps the shared view"
      (is (= [shared.views/group-field ::form tr/model->view tr/view->model]
             (ws.views/group-field ::form))))))

(deftest ^:unit description-field-test
  (testing "(description-field)"
    (testing "wraps the shared view"
      (is (= [shared.views/description-field ::form tr/model->view tr/view->model]
             (ws.views/description-field ::form))))))

(deftest ^:unit socket-test
  (testing "(socket)"
    (with-redefs [dates/format (spies/create (fn [v & _] v))
                  #?@(:cljs [interactions/show-send-modal (spies/constantly ::send-modal)
                             interactions/show-ws-modal (spies/constantly ::show-msg)
                             interactions/disconnect (spies/constantly ::disconnect)])]
      (let [messages [{:id :id-1 :body :body-1 :timestamp :timestamp-1 :message-id :message-id-1}
                      {:id :id-2 :body :body-2 :timestamp :timestamp-2 :message-id :message-id-2}
                      {:id :id-3 :body :body-3 :timestamp :timestamp-3 :message-id :message-id-3}]]
        (testing "when there are messages"
          (let [root (ws.views/socket ::simulator-id "socket-id" messages true)
                msg-trees (test.dom/query-all root :.ws-message)
                msg-count (count msg-trees)]
            (is (= (count messages) msg-count))
            (doseq [idx (range msg-count)
                    :let [tree (nth msg-trees idx)
                          num (inc idx)]]
              (testing (str "has message " num)
                (is (= (str ":id-" num) (:key (test.dom/attrs tree))))
                (is (spies/called-with? dates/format (keyword (str "timestamp-" num))))
                (is (test.dom/contains? tree (keyword (str "timestamp-" num))))
                (is (test.dom/contains? tree (keyword (str "body-" num)))))

              #?(:cljs
                 (testing "handles on-click"
                   (is (spies/called-with? interactions/show-ws-modal (nth messages idx)))
                   (is (-> tree
                           (test.dom/query-one :.ws-content)
                           (test.dom/attrs)
                           (:on-click)
                           (= ::show-msg))))))))

        (testing "when there are no messages"
          (let [root (ws.views/socket ::simulator-id "socket-id" nil true)]
            (is (-> root
                    (test.dom/query-one :.no-messages)
                    (test.dom/contains? "no messages")))
            (is (-> root
                    (test.dom/query-all :.ws-message)
                    (empty?)))))

        (testing "when the connection is active"
          (let [root (ws.views/socket ::simulator-id "socket-id" nil true)]
            (testing "can be styled as such"
              (is (test.dom/query-one root :.socket.active))
              (is (not (test.dom/query-one root :.socket.inactive)))
              #?@(:cljs
                  [(is (spies/called-with? interactions/show-send-modal ::simulator-id "socket-id"))
                   (is (-> root
                           (test.dom/query-one :.send-button)
                           (test.dom/attrs)
                           (:on-click)
                           (= ::send-modal)))
                   (is (-> root
                           (test.dom/query-one :.send-button)
                           (test.dom/attrs)
                           (:disabled)
                           (not)))
                   (is (spies/called-with? interactions/disconnect ::simulator-id "socket-id"))
                   (is (-> root
                           (test.dom/query-one :.disconnect-button)
                           (test.dom/attrs)
                           (:on-click)
                           (= ::disconnect)))
                   (is (-> root
                           (test.dom/query-one :.disconnect-button)
                           (test.dom/attrs)
                           (:disabled)
                           (not)))]))))

        (testing "when the connection is inactive"
          (let [root (ws.views/socket ::simulator-id "socket-id" nil false)]
            (testing "can be styled as such"
              (is (test.dom/query-one root :.socket.inactive))
              (is (not (test.dom/query-one root :.socket.active)))
              (is (-> root
                      (test.dom/query-one :.send-button)
                      (test.dom/attrs)
                      (:disabled)))
              (is (-> root
                      (test.dom/query-one :.disconnect-button)
                      (test.dom/attrs)
                      (:disabled))))))))))

(deftest ^:unit sim-edit-form*-test
  (testing "(sim-edit-form*)"
    (with-redefs [#?@(:cljs [forms/display-errors (spies/create)
                             forms/changed? (spies/constantly true)
                             interactions/update-simulator (spies/constantly ::submit)
                             interactions/reset-simulator (constantly ::reset)])]
      (let [root (ws.views/sim-edit-form* ::id ::form)
            form (test.dom/query-one root :.simulator-edit)]
        (testing "has a name field"
          (is (test.dom/contains? form [ws.views/name-field ::form true])))

        #?(:cljs
           (testing "when there are no errors and the form has changes"
             (testing "handles on submit"
               (is (spies/called-with? interactions/update-simulator ::form ::id))
               (is (= ::submit (:on-submit (test.dom/attrs form))))
               (is (-> form
                       (test.dom/query-one :.save-button)
                       (test.dom/attrs)
                       (:disabled)
                       (not)))))))

      #?(:cljs
         (testing "when there are errors"
           (spies/reset! interactions/update-simulator forms/display-errors forms/changed?)
           (spies/respond-with! forms/display-errors (constantly ::errors))
           (testing "form is not submittable"
             (let [root (ws.views/sim-edit-form* ::id ::form)]
               (is (-> root
                       (test.dom/query-one :.save-button)
                       (test.dom/attrs)
                       (:disabled)))))))

      #?(:cljs
         (testing "when the form has no changes"
           (spies/reset! interactions/update-simulator forms/display-errors forms/changed?)
           (spies/respond-with! forms/changed? (constantly false))
           (testing "form is not submittable"
             (let [root (ws.views/sim-edit-form* ::id ::form)]
               (is (-> root
                       (test.dom/query-one :.save-button)
                       (test.dom/attrs)
                       (:disabled))))))))))

(deftest ^:unit sim-edit-form-test
  (testing "(sim-edit-form)"
    (with-redefs [tr/sim->model (spies/constantly ::model)
                  #?@(:cljs [forms/create (spies/constantly ::form)])]
      (let [component (ws.views/sim-edit-form {:id ::id ::other ::things})]
        #?(:cljs
           (testing "creates a form"
             (is (spies/called-with? tr/sim->model {:id ::id ::other ::things}))
             (is (spies/called-with? forms/create ::model))))

        (testing "returns a component"
          (let [node (component ::simulator)]
            (is (= [ws.views/sim-edit-form* ::id #?(:clj ::model :cljs ::form)]
                   node))))))))

(deftest ^:unit sim-test
  (testing "(sim)"
    (with-redefs [#?@(:cljs [shared.interactions/clear-requests (spies/constantly ::clear)
                             interactions/disconnect-all (spies/constantly ::disconnect)
                             interactions/show-send-modal (spies/constantly ::send)
                             shared.interactions/show-delete-modal (constantly ::delete)])]
      (let [requests [{:socket-id 222 :timestamp 1}
                      {:socket-id 333 :timestamp 2}
                      {:socket-id 333 :timestamp 3}
                      {:socket-id 222 :timestamp 4}]
            sockets #{111 333}
            simulator {:sockets sockets :requests requests :id ::id}
            root (ws.views/sim simulator)]
        (testing "shows the simulator's details"
          (is (test.dom/contains? root [views.sim/sim-details simulator])))

        (testing "shows a form to edit the simulator"
          (is (test.dom/contains? root [ws.views/sim-edit-form simulator])))

        (testing "has a list of connections"
          (let [[socket-1 socket-2 socket-3] (test.dom/query-all root ws.views/socket)]
            (is (= [::id 111 [] true]
                   (rest socket-1)))
            (is (= "111" (:key (meta socket-1))))
            (is (= [::id 333 [{:socket-id 333 :timestamp 2} {:socket-id 333 :timestamp 3}] true]
                   (rest socket-2)))
            (is (= "333" (:key (meta socket-2))))
            (is (= [::id 222 [{:socket-id 222 :timestamp 1} {:socket-id 222 :timestamp 4}] false]
                   (rest socket-3)))
            (is (= "222" (:key (meta socket-3))))))

        #?(:cljs
           (testing "has a button to clear messages"
             (let [button (test.dom/query-one root :.clear-button)]
               (is (spies/called-with? shared.interactions/clear-requests :ws ::id))
               (is (-> button
                       (test.dom/attrs)
                       (:disabled)
                       (not)))
               (is (-> button
                       (test.dom/attrs)
                       (:on-click)
                       (= ::clear))))))

        #?(:cljs
           (testing "has a button to disconnect all sockets"
             (let [button (test.dom/query-one root :.disconnect-button)]
               (is (spies/called-with? interactions/disconnect-all ::id))
               (is (-> button
                       (test.dom/attrs)
                       (:disabled)
                       (not)))
               (is (-> button
                       (test.dom/attrs)
                       (:on-click)
                       (= ::disconnect))))))

        #?(:cljs
           (testing "has a button to broadcast a message"
             (let [button (test.dom/query-one root :.message-button)]
               (is (spies/called-with? interactions/show-send-modal ::id nil))
               (is (-> button
                       (test.dom/attrs)
                       (:disabled)
                       (not)))
               (is (-> button
                       (test.dom/attrs)
                       (:on-click)
                       (= ::send))))))

        #?(:cljs
           (testing "has a button to delete the simulator"
             (let [button (test.dom/query-one root :.delete-button)]
               (is (spies/called-with? shared.interactions/clear-requests :ws ::id))
               (is (-> button
                       (test.dom/attrs)
                       (:disabled)
                       (not)))
               (is (-> button
                       (test.dom/attrs)
                       (:on-click)
                       (= ::delete)))))))

      (testing "when there are no requests"
        (let [root (ws.views/sim {:requests [] :sockets #{111} :id ::id})]
          (testing "has a disabled clear button"
            (is (-> root
                    (test.dom/query-one :.clear-button)
                    (test.dom/attrs)
                    (:disabled))))))

      (testing "when there are no connections"
        (let [root (ws.views/sim {:requests [{:socket-id 111 :timestamp 1}] :sockets #{} :id ::id})]
          (testing "has a disabled disconnect button"
            (is (-> root
                    (test.dom/query-one :.disconnect-button)
                    (test.dom/attrs)
                    (:disabled))))

          (testing "has a disabled message button"
            (is (-> root
                    (test.dom/query-one :.message-button)
                    (test.dom/attrs)
                    (:disabled))))))

      (testing "when there are no requests and no connections"
        (let [root (ws.views/sim {:requests [] :sockets #{} :id ::id})]
          (testing "indicates there are no connections"
            (is (-> root
                    (test.dom/query-one :.no-sockets)
                    (test.dom/contains? "None")))))))))

(deftest ^:unit sim-create-form*-test
  (testing "(sim-create-form*)"
    (with-redefs [nav*/path-for (spies/constantly ::home)
                  #?@(:cljs [forms/display-errors (spies/create)
                             interactions/create-simulator (spies/constantly ::submit)])]
      (let [root (ws.views/sim-create-form* ::form)
            form (test.dom/query-one root :.simulator-create)]
        #?(:cljs
           (testing "can submit the form"
             (is (spies/called-with? forms/display-errors ::form))
             (is (spies/called-with? interactions/create-simulator ::form))
             (is (-> form
                     (test.dom/attrs)
                     (:on-submit)
                     (= ::submit)))))

        (testing "renders the path field"
          (is (-> form
                  (test.dom/query-one ws.views/path-field)
                  (= [ws.views/path-field ::form]))))

        (testing "renders the name field"
          (is (-> form
                  (test.dom/query-one ws.views/name-field)
                  (= [ws.views/name-field ::form]))))

        (testing "renders the group field"
          (is (-> form
                  (test.dom/query-one ws.views/group-field)
                  (= [ws.views/group-field ::form]))))

        (testing "renders the description field"
          (is (-> form
                  (test.dom/query-one ws.views/description-field)
                  (= [ws.views/description-field ::form]))))

        #?(:cljs
           (testing "renders an enabled save button"
             (is (-> form
                     (test.dom/query-one :.save-button)
                     (test.dom/attrs)
                     (:disabled)
                     (not)))))

        (testing "has a cancel link"
          (is (spies/called-with? nav*/path-for :home))
          (is (-> form
                  (test.dom/query-one :a.reset-button)
                  (test.dom/attrs)
                  (:href)
                  (= ::home)))))

      #?(:cljs
         (testing "when there are errors"
           (spies/respond-with! forms/display-errors (constantly ::errors))
           (spies/reset! interactions/create-simulator)
           (let [root (ws.views/sim-create-form* ::form)
                 form (test.dom/query-one root :.simulator-create)]
             (testing "disables the form"
               (is (-> form
                       (test.dom/query-one :.save-button)
                       (test.dom/attrs)
                       (:disabled))))))))))

(deftest ^:unit sim-create-form-test
  (testing "(sim-create-form)"
    (with-redefs [#?@(:cljs [forms/create (spies/constantly ::form)])]
      (let [component (ws.views/sim-create-form)
            model {:method :ws/ws :path "/"}]
        #?(:cljs
           (testing "creates a form"
             (is (spies/called-with? forms/create model resources/validate-new))))

        (testing "renders the create form"
          (let [root (component)]
            (is (-> root
                    (test.dom/query-one ws.views/sim-create-form*)
                    (= [ws.views/sim-create-form* #?(:clj model :cljs ::form)])))))))))

(defn run-tests []
  (t/run-tests))
