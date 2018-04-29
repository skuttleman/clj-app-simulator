(ns com.ben-allred.clj-app-simulator.ui.views.simulator-test
  (:require [cljs.test :as t :refer-macros [deftest testing is are async]]
            [com.ben-allred.clj-app-simulator.ui.views.simulator :as sim]
            [test.utils.dom :as test.dom]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]
            [com.ben-allred.clj-app-simulator.ui.utils.moment :as mo]
            [com.ben-allred.clj-app-simulator.ui.services.forms.core :as forms]
            [com.ben-allred.clj-app-simulator.ui.services.forms.fields :as fields]
            [com.ben-allred.clj-app-simulator.ui.utils.dom :as dom]
            [com.ben-allred.formation.core :as f]
            [com.ben-allred.clj-app-simulator.utils.strings :as strings]
            [cljs.core.async :as async]))

(defn ^:private pred [_ msg]
  (condp re-find msg
    #"whole" ::whole
    #"negative" ::positive
    #"number" ::number
    #"(?i)header.+key" ::header-key
    #"(?i)header.+value" ::header-value
    nil))

(deftest ^:unit validate*-test
  (testing "(validate*)"
    (let [pred-spy (spies/create pred)
          required-spy (spies/create (constantly ::required))
          coll-spy (spies/create (constantly ::coll))
          tuple-spy (spies/create (constantly ::tuple))
          validator-spy (spies/create (constantly ::validator))]
      (with-redefs [f/pred pred-spy
                    f/required required-spy
                    f/validator-coll coll-spy
                    f/validator-tuple tuple-spy
                    f/make-validator validator-spy]
        (let [validator (sim/validate*)
              validator-map (ffirst (spies/calls validator-spy))]
          (testing "makes a validator"
            (is (= ::validator validator))
            (is (spies/called-times? pred-spy 5)))

          (testing "validates :delay"
            (let [delay (:delay validator-map)]
              (is (= (set delay) #{::number ::whole ::positive}))))

          (testing "when validating :response :status"
            (is (spies/called? required-spy))
            (is (= ::required (get-in validator-map [:response :status]))))

          (testing "when validating :response :headers"
            (is (spies/called-with? tuple-spy ::header-key ::header-value))
            (is (spies/called-with? coll-spy ::tuple))
            (is (= ::coll (get-in validator-map [:response :headers])))))))))

(deftest ^:unit reset-sim-test
  (testing "(reset-sim)"
    (async done
      (async/go
        (let [chan (async/chan)
              request-spy (spies/create (constantly chan))
              reset-spy (spies/create)
              response-ch (sim/reset-sim request-spy reset-spy)]
          (testing "resets the simulator"
            (async/put! chan [:success {:config {:new :config :delay 13}}])
            (async/<! response-ch)

            (is (spies/called-with? request-spy))
            (is (spies/called-with? reset-spy {:delay 13}))

            (done)))))))

(deftest ^:unit sim-iterate-test
  (testing "(sim-iterate)"
    (let [xform-spy (spies/create identity)
          root (sim/sim-iterate "a label"
                                {:two   ["a" "list" "of" "things"]
                                 :one   "thing"
                                 :three "another thing"}
                                ::class
                                xform-spy)]
      (testing "uses class on root node"
        (is (= ::class (:class-name (test.dom/attrs root)))))
      (testing "displays the label"
        (is (test.dom/contains? root "a label")))
      (testing "when displaying key-vals"
        (let [key-vals (test.dom/query-one root :.key-vals)
              [li-1 li-2 li-3] (test.dom/query-all key-vals :li)]
          (testing "sorts by key"
            (is (= ":one" (:key (test.dom/attrs li-1))))
            (is (= ":two" (:key (test.dom/attrs li-3))))
            (is (= ":three" (:key (test.dom/attrs li-2)))))
          (testing "displays the transformed key"
            (is (spies/called-with? xform-spy "one"))
            (is (test.dom/contains? (test.dom/query-one li-1 :.key) "one"))
            (is (spies/called-with? xform-spy "two"))
            (is (test.dom/contains? (test.dom/query-one li-3 :.key) "two"))
            (is (spies/called-with? xform-spy "three"))
            (is (test.dom/contains? (test.dom/query-one li-2 :.key) "three")))
          (testing "displays the val"
            (is (-> li-1
                    (test.dom/query-one :.val)
                    (test.dom/contains? "thing")))
            (is (-> li-2
                    (test.dom/query-one :.val)
                    (test.dom/contains? "another thing"))))
          (testing "and when the val is a collection"
            (testing "joins the coll"
              (is (-> li-3
                      (test.dom/query-one :.val)
                      (test.dom/contains? "a,list,of,things"))))))))))

(deftest ^:unit sim-details-test
  (testing "(sim-details)"
    (let [root (sim/sim-details {:config {:method ::method :path ::path}})]
      (testing "displays the method"
        (is (-> root
                (test.dom/query-one :.sim-card-method)
                (test.dom/contains? "METHOD"))))
      (testing "displays the path"
        (is (-> root
                (test.dom/query-one :.sim-card-path)
                (test.dom/contains? ::path)))))))

(deftest ^:unit name-field-test
  (testing "(name-field)"
    (let [assoc-spy (spies/create)
          current-spy (spies/create (constantly {:name ::value}))
          error-spy (spies/create (constantly {:name ::error}))]
      (with-redefs [forms/assoc-in assoc-spy
                    forms/current-model current-spy
                    forms/errors error-spy]
        (let [root (sim/name-field ::form)
              input (test.dom/query-one root fields/input)
              attrs (test.dom/attrs input)]
          (testing "renders input field"
            (is input))

          (testing "has a :label"
            (is (= "Name" (:label attrs))))

          (testing "has an :on-change function which updates the form"
            (let [on-change (:on-change attrs)]
              (on-change ::some-value)
              (spies/called-with? assoc-spy ::form [:name] ::some-value)))

          (testing "has a :value"
            (is (= ::value (:value attrs))))

          (testing "has :errors"
            (is (= ::error (:errors attrs)))))))))

(deftest ^:unit group-field-test
  (testing "(group-field)"
    (let [assoc-spy (spies/create)
          current-spy (spies/create (constantly {:group ::value}))
          error-spy (spies/create (constantly {:group ::error}))]
      (with-redefs [forms/assoc-in assoc-spy
                    forms/current-model current-spy
                    forms/errors error-spy]
        (let [root (sim/group-field ::form)
              input (test.dom/query-one root fields/input)
              attrs (test.dom/attrs input)]
          (testing "renders input field"
            (is input))

          (testing "has a :label"
            (is (= "Group" (:label attrs))))

          (testing "has an :on-change function which updates the form"
            (let [on-change (:on-change attrs)]
              (on-change ::some-value)
              (spies/called-with? assoc-spy ::form [:group] ::some-value)))

          (testing "has a :value"
            (is (= ::value (:value attrs))))

          (testing "has :errors"
            (is (= ::error (:errors attrs)))))))))

(deftest ^:unit description-field-test
  (testing "(description-field)"
    (let [assoc-spy (spies/create)
          current-spy (spies/create (constantly {:description ::description}))
          error-spy (spies/create (constantly {:description ::errors}))]
      (with-redefs [forms/assoc-in assoc-spy
                    forms/current-model current-spy
                    forms/errors error-spy]
        (let [root (sim/description-field ::form)
              textarea (test.dom/query-one root fields/textarea)
              attrs (test.dom/attrs textarea)]
          (testing "renders textarea field"
            (is textarea))

          (testing "has a :label"
            (is (= "Description" (:label attrs))))

          (testing "has an :on-change function which updates the form"
            (let [on-change (:on-change attrs)]
              (on-change ::some-value)
              (spies/called-with? assoc-spy ::form [:description] ::some-value)))

          (testing "has a :value"
            (is (= ::description (:value attrs))))

          (testing "has :errors"
            (is (= ::errors (:errors attrs)))))))))

(deftest ^:unit status-field-test
  (testing "(status-field)"
    (let [assoc-spy (spies/create)
          current-spy (spies/create (constantly {:response {:status ::status}}))
          error-spy (spies/create (constantly {:response {:status ::errors}}))]
      (with-redefs [forms/assoc-in assoc-spy
                    forms/current-model current-spy
                    forms/errors error-spy
                    sim/statuses ::statuses]
        (let [root (sim/status-field ::form)
              select (test.dom/query-one root fields/select)
              attrs (test.dom/attrs select)]
          (testing "renders select field"
            (is (= ::statuses (last select))))

          (testing "has a :label"
            (is (= "Status" (:label attrs))))

          (testing "has an :on-change function which updates the form"
            (let [on-change (:on-change attrs)]
              (on-change ::some-value)
              (is (spies/called-with? assoc-spy ::form [:response :status] ::some-value))))

          (testing "has a :value"
            (is (= ::status (:value attrs))))

          (testing "has a :to-view function"
            (let [to-view (:to-view attrs)]
              (is (= "200" (to-view 200)))))

          (testing "has a :to-model function"
            (let [to-model (:to-model attrs)]
              (is (= 200 (to-model "200")))))

          (testing "has :errors"
            (is (= ::errors (:errors attrs)))))))))

(deftest ^:unit delay-field-test
  (testing "(delay-field)"
    (let [assoc-spy (spies/create)
          current-spy (spies/create (constantly {:delay ::value}))
          error-spy (spies/create (constantly {:delay ::error}))]
      (with-redefs [forms/assoc-in assoc-spy
                    forms/current-model current-spy
                    forms/errors error-spy]
        (let [root (sim/delay-field ::form)
              input (test.dom/query-one root fields/input)
              attrs (test.dom/attrs input)]
          (testing "renders input field"
            (is input))

          (testing "has a :label"
            (is (= "Delay (ms)" (:label attrs))))

          (testing "has an :on-change function which updates the form"
            (let [on-change (:on-change attrs)]
              (on-change ::some-value)
              (spies/called-with? assoc-spy ::form [:delay] ::some-value)))

          (testing "has a :to-view function which formats the delay"
            (let [to-view (:to-view attrs)]
              (is (= "0" (to-view 0)))))

          (testing "has a :to-model function which parses values when possible"
            (let [to-model (:to-model attrs)]
              (is (= 123 (to-model "0123")))
              (is (= "not-parsed" (to-model "not-parsed")))))

          (testing "has a :value"
            (is (= ::value (:value attrs))))

          (testing "has :errors"
            (is (= ::error (:errors attrs)))))))))

(deftest ^:unit headers-field-test
  (testing "(headers-field)"
    (let [update-spy (spies/create)
          current-spy (spies/create (constantly {:response {:headers ::headers}}))
          error-spy (spies/create (constantly {:response {:headers ::errors}}))]
      (with-redefs [forms/update-in update-spy
                    forms/current-model current-spy
                    forms/errors error-spy]
        (let [root (sim/headers-field ::form)
              multi (test.dom/query-one root fields/multi)
              attrs (test.dom/attrs multi)]
          (testing "renders multi field"
            (is (= fields/header (last multi))))

          (testing "has a :label"
            (is (= "Headers" (:label attrs))))

          (testing "has a :key-fn function which produces a key"
            (let [key-fn (:key-fn attrs)]
              (is (= "header-key" (key-fn ["key" ::whatever])))))

          (testing "has a :new-fn function"
            (let [new-fn (:new-fn attrs)]
              (is (= ["" ""] (new-fn ::anything-at-all)))))

          (testing "has a :change-fn function which updates the form"
            (let [change-fn (:change-fn attrs)]
              (change-fn :a :b :c)
              (is (spies/called-with? update-spy ::form [:response :headers] :a :b :c))))

          (testing "has a :value"
            (is (= ::headers (:value attrs))))

          (testing "has a :to-view function"
            (let [to-view (:to-view attrs)]
              (is (= ["Some-Header" "some-value"] (to-view [:some-header "some-value"])))))

          (testing "has a :to-model function"
            (let [to-model (:to-model attrs)]
              (is (= [:some-header "some-value"] (to-model ["Some-Header" "some-value"])))))

          (testing "has :errors"
            (is (= ::errors (:errors attrs)))))))))

(deftest ^:unit body-field-test
  (testing "(body-field)"
    (let [assoc-spy (spies/create)
          current-spy (spies/create (constantly {:response {:body ::body}}))
          error-spy (spies/create (constantly {:response {:body ::errors}}))]
      (with-redefs [forms/assoc-in assoc-spy
                    forms/current-model current-spy
                    forms/errors error-spy]
        (let [root (sim/body-field ::form)
              textarea (test.dom/query-one root fields/textarea)
              attrs (test.dom/attrs textarea)]
          (testing "renders textarea field"
            (is textarea))

          (testing "has a :label"
            (is (= "Body" (:label attrs))))

          (testing "has an :on-change function which updates the form"
            (let [on-change (:on-change attrs)]
              (on-change ::some-value)
              (spies/called-with? assoc-spy ::form [:response :body] ::some-value)))

          (testing "has a :value"
            (is (= ::body (:value attrs))))

          (testing "has :errors"
            (is (= ::errors (:errors attrs)))))))))

(deftest ^:unit sim-edit-form*-test
  (testing "(sim-edit-form*)"
    (let [model {:response {:status ::status
                            :body   ""}
                 :name     nil}
          errors-spy (spies/create)
          changed-spy (spies/create)
          model-spy (spies/create (constantly model))
          event-spy (spies/create)
          action-spy (spies/create (constantly ::action))
          dispatch-spy (spies/create)]
      (with-redefs [forms/errors errors-spy
                    forms/changed? changed-spy
                    forms/current-model model-spy
                    dom/prevent-default event-spy
                    actions/update-simulator action-spy
                    store/dispatch dispatch-spy]
        (testing "when rendering a form"
          (let [root (sim/sim-edit-form* ::id ::form)
                edit-form (test.dom/query-one root :.simulator-edit)]
            (testing "renders a name field"
              (let [node (test.dom/query-one edit-form sim/name-field)]
                (is (= [sim/name-field ::form] node))))

            (testing "renders a group field"
              (let [node (test.dom/query-one edit-form sim/group-field)]
                (is (= [sim/group-field ::form] node))))

            (testing "renders a description field"
              (let [node (test.dom/query-one edit-form sim/description-field)]
                (is (= [sim/description-field ::form] node))))

            (testing "renders a status field"
              (let [node (test.dom/query-one edit-form sim/status-field)]
                (is (= [sim/status-field ::form] node))))

            (testing "renders a delay field"
              (let [node (test.dom/query-one edit-form sim/delay-field)]
                (is (= [sim/delay-field ::form] node))))

            (testing "renders a headers field"
              (let [node (test.dom/query-one edit-form sim/headers-field)]
                (is (= [sim/headers-field ::form] node))))

            (testing "renders a body field"
              (let [node (test.dom/query-one edit-form sim/body-field)]
                (is (= [sim/body-field ::form] node))))

            (testing "when resetting the simulator"
              (spies/reset! dispatch-spy action-spy)
              (testing "dispatches an action"))))))))

(deftest ^:unit sim-edit-form*-test--submit
  (testing "(sim-edit-form*)/submit"
    (let [model {:response {:headers [[:header "value-1"] [:header "value-2"]]}}
          expected-data {:response {:headers {:header ["value-1" "value-2"]}}}
          dispatch-spy (spies/create)
          action-spy (spies/create (constantly ::action))
          reset-spy (spies/create)
          errors-spy (spies/create)
          changed-spy (spies/create)
          current-spy (spies/create (constantly model))
          default-spy (spies/create)]
      (with-redefs [store/dispatch dispatch-spy
                    actions/update-simulator action-spy
                    forms/reset! reset-spy
                    forms/errors errors-spy
                    forms/changed? changed-spy
                    forms/current-model current-spy
                    dom/prevent-default default-spy]
        (testing "when submitting the form"
          (testing "and when there are errors"
            (spies/reset! dispatch-spy action-spy default-spy reset-spy)
            (spies/respond-with! errors-spy (constantly ::errors))
            (spies/respond-with! changed-spy (constantly true))

            (let [root (sim/sim-edit-form* ::id ::form)
                  edit-form (test.dom/query-one root :.simulator-edit)]
              (testing "has a disabled submit button"
                (is (-> edit-form
                        (test.dom/query-one :.save-button)
                        (test.dom/attrs)
                        (:disabled))))

              (testing "does not submit the form"
                (is (spies/never-called? action-spy))
                (is (spies/never-called? dispatch-spy))
                (is (spies/never-called? reset-spy)))))

          (testing "and when there are no changes"
            (spies/reset! dispatch-spy action-spy default-spy reset-spy)
            (spies/respond-with! errors-spy (constantly nil))
            (spies/respond-with! changed-spy (constantly false))

            (let [root (sim/sim-edit-form* ::id ::form)
                  edit-form (test.dom/query-one root :.simulator-edit)]
              (testing "has a disabled submit button"
                (is (-> edit-form
                        (test.dom/query-one :.save-button)
                        (test.dom/attrs)
                        (:disabled))))

              (testing "does not submit the form"
                (is (spies/never-called? action-spy))
                (is (spies/never-called? dispatch-spy))
                (is (spies/never-called? reset-spy)))))

          (testing "and when there are changes and no errors"
            (spies/reset! dispatch-spy action-spy default-spy reset-spy)
            (spies/respond-with! errors-spy (constantly nil))
            (spies/respond-with! changed-spy (constantly true))

            (let [root (sim/sim-edit-form* ::id ::form)
                  edit-form (test.dom/query-one root :.simulator-edit)]
              (test.dom/simulate-event edit-form :submit ::event)

              (testing "has an enabled submit button"
                (is (-> edit-form
                        (test.dom/query-one :.save-button)
                        (test.dom/attrs)
                        (:disabled)
                        (not))))

              (testing "prevents the default form behavior"
                (is (spies/called-with? default-spy ::event)))

              (testing "dispatches an action"
                (is (spies/called-with? action-spy ::id expected-data))
                (is (spies/called-with? dispatch-spy ::action)))

              (testing "resets the form"
                (is (spies/called-with? reset-spy ::form model))))))))))

(deftest ^:unit sim-edit-form*-test--reset
  (testing "(sim-edit-form*)/reset"
    (let [reset-spy (spies/create)
          errors-spy (spies/create)
          changed-spy (spies/create)
          form-reset-spy (spies/create)
          dispatch-spy (spies/create)
          action-spy (spies/create (constantly ::action))]
      (testing "resets the simulator"
        (with-redefs [sim/reset-sim reset-spy
                      forms/errors errors-spy
                      forms/changed? changed-spy
                      forms/reset! form-reset-spy
                      store/dispatch dispatch-spy
                      actions/reset-simulator action-spy]
          (-> (sim/sim-edit-form* ::id ::form)
              (test.dom/query-one :.simulator-edit)
              (test.dom/query-one :.reset-button)
              (test.dom/simulate-event :click))
          (spies/reset! errors-spy changed-spy form-reset-spy dispatch-spy action-spy)

          (let [[request reset] (first (spies/calls reset-spy))]
            (request)
            (is (spies/called-with? action-spy ::id))
            (is (spies/called-with? dispatch-spy ::action))

            (reset)
            (is (spies/called-with? form-reset-spy ::form))))))))

(deftest ^:unit sim-edit-form-test
  (testing "(sim-edit-form)"
    (let [form-spy (spies/create (constantly ::form))
          simulator {:config {:useless     ::thing
                              :also        ::useless
                              :group       ::group
                              :name        ::name
                              :description ::description
                              :response    {:status  ::status
                                            :body    ::body
                                            :headers {:header-c ["header-c"]
                                                      :header-a "thing"
                                                      :header-b ["double" "things"]}}}
                     :id     ::id}]
      (with-redefs [forms/create form-spy]
        (let [root (sim/sim-edit-form simulator)
              expected {:group       ::group
                        :name        ::name
                        :description ::description
                        :delay       0
                        :response    {:status  ::status
                                      :body    ::body
                                      :headers [[:header-a "thing"]
                                                [:header-b "double"]
                                                [:header-b "things"]
                                                [:header-c "header-c"]]}}]
          (testing "creates a form from source data"
            (is (spies/called-with? form-spy expected sim/validate)))

          (testing "returns a function that renders the form"
            (is (= [sim/sim-edit-form* ::id ::form]
                   (root simulator)))))))))

(deftest ^:unit request-modal-test
  (testing "(request-modal)"
    (let [format-spy (spies/create (constantly ::formatted))]
      (with-redefs [mo/format format-spy]
        (let [qp {:a 1 :b 2}
              headers {:header-1 "val-1" :header-2 "val-2"}
              root (sim/request-modal {:method ::method :path "/some/path"}
                                      {:dt           ::moment
                                       :query-params qp
                                       :headers      headers
                                       :body         "this is a body"})
              details (test.dom/query-one root :.request-details)]
          (testing "displays the method and path"
            (is (test.dom/contains? details [:* "METHOD" ": " "/some/path"])))

          (testing "formats the dt moment"
            (is (spies/called-with? format-spy ::moment)))

          (testing "displays the formatted moment"
            (is (test.dom/contains? details ::formatted)))

          (testing "when there are query params"
            (testing "iterates over query params"
              (is (test.dom/contains? details
                                      [sim/sim-iterate "Query:" qp "query-params"]))))

          (testing "when there are headers"
            (testing "iterates over headers"
              (is (test.dom/contains? details
                                      [sim/sim-iterate "Headers:" headers "headers" strings/titlize]))))

          (testing "when there is a body"
            (let [request-body (test.dom/query-one details :.request-body)]
              (testing "displays a label for the body"
                (is (test.dom/contains? request-body "Body:")))

              (testing "displays the body"
                (is (test.dom/contains? request-body "this is a body"))))))

        (testing "when there are no query params"
          (let [root (sim/request-modal {:method ::method} {})
                iterate (test.dom/query-all root sim/sim-iterate)]
            (testing "does not iterate query-params"
              (is (not (test.dom/contains? iterate "Query:")))
              (is (not (test.dom/contains? iterate "query-params"))))))

        (testing "when there are no headers"
          (let [root (sim/request-modal {:method ::method} {})
                iterate (test.dom/query-all root sim/sim-iterate)]
            (testing "does not iterate headers"
              (is (not (test.dom/contains? iterate "Headers:")))
              (is (not (test.dom/contains? iterate "headers"))))))

        (testing "when there is no body"
          (let [details (sim/request-modal {:method ::method} {})]
            (testing "does not display a body"
              (is (not (test.dom/contains? details :.request-body))))))))))

(deftest ^:unit sim-request-test
  (testing "(sim-request)"
    (let [moment-spy (spies/create (constantly ::moment))
          from-now-spy (spies/create (constantly ::from-now))
          action-spy (spies/create (constantly ::action))
          dispatch-spy (spies/create)]
      (with-redefs [mo/->moment moment-spy
                    mo/from-now from-now-spy
                    actions/show-modal action-spy
                    store/dispatch dispatch-spy]
        (let [request {:timestamp ::timestamp :details ::details}
              root (sim/sim-request ::sim request)
              tree (test.dom/query-one root :.request)]
          (testing "converts timestamp to moment"
            (is (spies/called-with? moment-spy ::timestamp)))

          (testing "when clicking the tree"
            (spies/reset! action-spy dispatch-spy)
            (test.dom/simulate-event tree :click)
            (testing "shows the modal"
              (is (spies/called-with? action-spy
                                      [sim/request-modal ::sim (assoc request :dt ::moment)]
                                      "Request Details"))
              (is (spies/called-with? dispatch-spy ::action))))

          (testing "displays moment from now"
            (is (test.dom/contains? tree ::from-now))))))))

(deftest ^:unit sim-test
  (testing "(sim)"
    (let [sim {:id       ::simulator-id
               :config   {::some ::simulator}
               :requests [{:timestamp 123
                           ::data     ::123}
                          {:timestamp 456
                           ::data     ::456}]}
          root (sim/sim sim)
          clear-spy (spies/create (constantly ::clear))
          dispatch-spy (spies/create)]
      (testing "displays sim-details"
        (let [details (test.dom/query-one root sim/sim-details)]
          (is (= [sim/sim-details sim] details))))

      (testing "displays sim-edit-form"
        (let [form (test.dom/query-one root sim/sim-edit-form)]
          (is (= [sim/sim-edit-form sim] form))))

      (testing "displays a list of sim-request components"
        (let [[req-1 req-2 :as sim-reqs] (test.dom/query-all root sim/sim-request)]
          (is (= 2 (count sim-reqs)))
          (is (= "456" (:key (meta req-1))))
          (is (= [sim/sim-request {::some ::simulator} {:timestamp 456 ::data ::456}]
                 req-1))
          (is (= "123" (:key (meta req-2))))
          (is (= [sim/sim-request {::some ::simulator} {:timestamp 123 ::data ::123}]
                 req-2))))

      (with-redefs [actions/clear-requests clear-spy
                    store/dispatch dispatch-spy]
        (testing "has a button to clear requests"
          (let [button (test.dom/query-one root :.button.clear-button)]
            (spies/reset! clear-spy dispatch-spy)
            (is button)
            (test.dom/simulate-event button :click)
            (is (spies/called-with? clear-spy ::simulator-id))
            (is (spies/called-with? dispatch-spy ::clear))
            (testing "when there are no requests"
              (testing "the button is not disabled"
                (is (not (:disabled (test.dom/attrs button)))))))

          (testing "when there are no requests"
            (let [root (sim/sim {})
                  button (test.dom/query-one root :.button.clear-button)]
              (testing "the button is disabled"
                (is (:disabled (test.dom/attrs button)))))))))))

(defn run-tests [] (t/run-tests))
