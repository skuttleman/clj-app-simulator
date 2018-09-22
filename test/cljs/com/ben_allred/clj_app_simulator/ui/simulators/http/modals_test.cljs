(ns com.ben-allred.clj-app-simulator.ui.simulators.http.modals-test
  (:require [clojure.test :as t :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.templates.fields :as fields]
            [com.ben-allred.clj-app-simulator.ui.simulators.http.modals :as modals]
            [com.ben-allred.clj-app-simulator.templates.views.forms.shared :as shared.views]
            [test.utils.dom :as test.dom]
            [test.utils.spies :as spies]))

(deftest ^:unit message-test
  (testing "(message)"
    (let [with-attrs-spy (spies/create identity)]
      (with-redefs [shared.views/with-attrs with-attrs-spy]
        (let [root (modals/message ::form ::model->view ::view->model)
              input (test.dom/query-one root fields/textarea)]
          (testing "renders a message field"
            (is (spies/called-with? with-attrs-spy (spies/matcher map?) ::form [:message] ::model->view ::view->model))
            (is (= "Message" (:label (test.dom/attrs input))))))))))

(defn run-tests []
  (t/run-tests))
