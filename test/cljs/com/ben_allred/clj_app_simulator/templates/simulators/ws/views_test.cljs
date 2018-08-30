(ns com.ben-allred.clj-app-simulator.templates.simulators.ws.views-test
  (:require [clojure.test :as t :refer [deftest testing is] :include-macros true]
            [com.ben-allred.clj-app-simulator.templates.simulators.ws.transformations :as tr]
            [com.ben-allred.clj-app-simulator.templates.simulators.shared.views :as shared.views]
            [com.ben-allred.clj-app-simulator.templates.simulators.ws.views :as ws.views]
            [test.utils.spies :as spies]
            [test.utils.dom :as test.dom]
            [com.ben-allred.clj-app-simulator.utils.datetime :as dt]
            [com.ben-allred.clj-app-simulator.services.forms :as forms]
            [com.ben-allred.clj-app-simulator.ui.simulators.ws.interactions :as interactions]
            [com.ben-allred.clj-app-simulator.ui.simulators.shared.interactions :as shared.interactions]
            [com.ben-allred.clj-app-simulator.templates.simulators.ws.resources :as resources]
            [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]))

(deftest ^:unit name-field-test
  (testing "(name-field)"
    (testing "wraps the shared view"
      (is (= [shared.views/name-field ::form tr/model->view tr/view->model]
             (ws.views/name-field ::form))))))

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
    (let [from-now-spy (spies/create identity)
          send-spy (spies/constantly ::send-modal)
          disconnect-spy (spies/constantly ::disconnect)]
      (with-redefs [dt/format from-now-spy
                    interactions/show-message-modal send-spy
                    interactions/disconnect disconnect-spy]
        (let [messages [{:body ::body-1 :timestamp :timestamp-1}
                        {:body ::body-2 :timestamp :timestamp-2}
                        {:body ::body-3 :timestamp :timestamp-3}]]
          (testing "when there are messages"
            (let [root (ws.views/socket ::simulator-id "socket-id" messages true)
                  [message-1 message-2 message-3] (test.dom/query-all root :.ws-message)]
              (is (= "socket-id-:timestamp-1" (:key (test.dom/attrs message-1))))
              (is (spies/called-with? from-now-spy :timestamp-1))
              (is (test.dom/contains? message-1 :timestamp-1))
              (is (test.dom/contains? message-1 ::body-1))

              (is (= "socket-id-:timestamp-2" (:key (test.dom/attrs message-2))))
              (is (spies/called-with? from-now-spy :timestamp-2))
              (is (test.dom/contains? message-2 :timestamp-2))
              (is (test.dom/contains? message-2 ::body-2))

              (is (= "socket-id-:timestamp-3" (:key (test.dom/attrs message-3))))
              (is (spies/called-with? from-now-spy :timestamp-3))
              (is (test.dom/contains? message-3 :timestamp-3))
              (is (test.dom/contains? message-3 ::body-3))))

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
                (is (spies/called-with? send-spy ::simulator-id "socket-id"))
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
                (is (spies/called-with? disconnect-spy ::simulator-id "socket-id"))
                (is (-> root
                        (test.dom/query-one :.disconnect-button)
                        (test.dom/attrs)
                        (:on-click)
                        (= ::disconnect)))
                (is (-> root
                        (test.dom/query-one :.disconnect-button)
                        (test.dom/attrs)
                        (:disabled)
                        (not))))))

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
                        (:disabled)))))))))))

(deftest ^:unit sim-edit-form*-test
  (testing "(sim-edit-form*)"
    (let [errors-spy (spies/create)
          changed-spy (spies/constantly true)
          update-spy (spies/constantly ::submit)
          reset-spy (spies/constantly ::reset)]
      (with-redefs [forms/errors errors-spy
                    forms/changed? changed-spy
                    interactions/update-simulator update-spy
                    interactions/reset-simulator reset-spy]
        (let [root (ws.views/sim-edit-form* ::id ::form)
              form (test.dom/query-one root :.simulator-edit)]
          (testing "has a name field"
            (is (test.dom/contains? form [ws.views/name-field ::form])))

          (testing "when there are no errors and the form has changes"
            (testing "handles on submit"
              (is (spies/called-with? update-spy ::form ::id true))
              (is (= ::submit (:on-submit (test.dom/attrs form))))
              (is (-> form
                      (test.dom/query-one :.save-button)
                      (test.dom/attrs)
                      (:disabled)
                      (not))))))

        (testing "when there are errors"
          (spies/reset! update-spy errors-spy changed-spy)
          (spies/respond-with! errors-spy (constantly ::errors))
          (testing "form is not submittable"
            (let [root (ws.views/sim-edit-form* ::id ::form)]
              (is (spies/called-with? update-spy ::form ::id false))
              (is (-> root
                      (test.dom/query-one :.save-button)
                      (test.dom/attrs)
                      (:disabled))))))

        (testing "when the form has no changes"
          (spies/reset! update-spy errors-spy changed-spy)
          (spies/respond-with! changed-spy (constantly false))
          (testing "form is not submittable"
            (let [root (ws.views/sim-edit-form* ::id ::form)]
              (is (spies/called-with? update-spy ::form ::id false))
              (is (-> root
                      (test.dom/query-one :.save-button)
                      (test.dom/attrs)
                      (:disabled))))))))))

(deftest ^:unit sim-edit-form-test
  (testing "(sim-edit-form)"
    (let [model-spy (spies/constantly ::model)
          form-spy (spies/constantly ::form)]
      (with-redefs [tr/sim->model model-spy
                    forms/create form-spy]
        (testing "creates a form"
          (let [component (ws.views/sim-edit-form {:id ::id ::other ::things})]
            (is (spies/called-with? model-spy {:id ::id ::other ::things}))
            (is (spies/called-with? form-spy ::model))

            (testing "returns a component"
              (let [node (component ::simulator)]
                (is (= [ws.views/sim-edit-form* ::id ::form]
                       node))))))))))

(deftest ^:unit sim-test
  (testing "(sim)"
    (let [clear-spy (spies/constantly ::clear)
          disconnect-spy (spies/constantly ::disconnect)
          send-spy (spies/constantly ::send)
          delete-spy (spies/constantly ::delete)]
      (with-redefs [shared.interactions/clear-requests clear-spy
                    interactions/disconnect-all disconnect-spy
                    interactions/show-message-modal send-spy
                    shared.interactions/show-delete-modal delete-spy]
        (let [requests [{:socket-id 222 :timestamp 1}
                        {:socket-id 333 :timestamp 2}
                        {:socket-id 333 :timestamp 3}
                        {:socket-id 222 :timestamp 4}]
              sockets #{111 333}
              simulator {:sockets sockets :requests requests :id ::id}
              root (ws.views/sim simulator)]
          (testing "shows the simulator's details"
            (is (test.dom/contains? root [shared.views/sim-details simulator])))

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

          (testing "has a button to clear messages"
            (let [button (test.dom/query-one root :.clear-button)]
              (is (spies/called-with? clear-spy ::id))
              (is (-> button
                      (test.dom/attrs)
                      (:disabled)
                      (not)))
              (is (-> button
                      (test.dom/attrs)
                      (:on-click)
                      (= ::clear)))))

          (testing "has a button to disconnect all sockets"
            (let [button (test.dom/query-one root :.disconnect-button)]
              (is (spies/called-with? disconnect-spy ::id))
              (is (-> button
                      (test.dom/attrs)
                      (:disabled)
                      (not)))
              (is (-> button
                      (test.dom/attrs)
                      (:on-click)
                      (= ::disconnect)))))

          (testing "has a button to broadcast a message"
            (let [button (test.dom/query-one root :.message-button)]
              (is (spies/called-with? send-spy ::id nil))
              (is (-> button
                      (test.dom/attrs)
                      (:disabled)
                      (not)))
              (is (-> button
                      (test.dom/attrs)
                      (:on-click)
                      (= ::send)))))

          (testing "has a button to delete the simulator"
            (let [button (test.dom/query-one root :.delete-button)]
              (is (spies/called-with? clear-spy ::id))
              (is (-> button
                      (test.dom/attrs)
                      (:disabled)
                      (not)))
              (is (-> button
                      (test.dom/attrs)
                      (:on-click)
                      (= ::delete))))))

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
                      (test.dom/contains? "None"))))))))))

(deftest ^:unit sim-create-form*-test
  (testing "(sim-create-form*)"
    (let [errors-spy (spies/create)
          create-spy (spies/constantly ::submit)
          nav-spy (spies/constantly ::home)]
      (with-redefs [forms/errors errors-spy
                    interactions/create-simulator create-spy
                    nav/path-for nav-spy]
        (let [root (ws.views/sim-create-form* ::form)
              form (test.dom/query-one root :.simulator-create)]
          (testing "can submit the form"
            (is (spies/called-with? errors-spy ::form))
            (is (spies/called-with? create-spy ::form true))
            (is (-> form
                    (test.dom/attrs)
                    (:on-submit)
                    (= ::submit))))

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

          (testing "renders an enabled save button"
            (is (-> form
                    (test.dom/query-one :.save-button)
                    (test.dom/attrs)
                    (:disabled)
                    (not))))

          (testing "has a cancel link"
            (is (spies/called-with? nav-spy :home))
            (is (-> form
                    (test.dom/query-one :a.reset-button)
                    (test.dom/attrs)
                    (:href)
                    (= ::home)))))

        (testing "when there are errors"
          (spies/respond-with! errors-spy (constantly ::errors))
          (spies/reset! create-spy)
          (let [root (ws.views/sim-create-form* ::form)
                form (test.dom/query-one root :.simulator-create)]
            (testing "disables the form"
              (is (spies/called-with? create-spy ::form false))
              (is (-> form
                      (test.dom/query-one :.save-button)
                      (test.dom/attrs)
                      (:disabled))))))))))

(deftest ^:unit sim-create-form-test
  (testing "(sim-create-form)"
    (let [form-spy (spies/constantly ::form)]
      (with-redefs [forms/create form-spy]
        (let [component (ws.views/sim-create-form nil)]
          (testing "creates a form"
            (is (spies/called-with? form-spy {:method :ws :path "/"} resources/validate-new)))

          (testing "renders the create form"
            (let [root (component)]
              (is (-> root
                      (test.dom/query-one ws.views/sim-create-form*)
                      (= [ws.views/sim-create-form* ::form]))))))))))

(defn run-tests []
  (t/run-tests))
