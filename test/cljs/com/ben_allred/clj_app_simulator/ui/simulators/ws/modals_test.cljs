(ns com.ben-allred.clj-app-simulator.ui.simulators.ws.modals-test
  (:require [clojure.test :as t :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.templates.components.form-fields :as ff]
            [com.ben-allred.clj-app-simulator.ui.simulators.ws.modals :as modals]
            [com.ben-allred.clj-app-simulator.templates.simulators.shared.views :as shared.views]
            [test.utils.dom :as test.dom]
            [test.utils.spies :as spies]))

(deftest ^:unit message-test
  (testing "(message)"
    (let [with-attrs-spy (spies/create identity)]
      (with-redefs [shared.views/with-attrs with-attrs-spy]
        (let [root (modals/message ::form)
              input (test.dom/query-one root ff/textarea)]
          (testing "renders a message field"
            (is (spies/called-with? with-attrs-spy (spies/matcher map?) ::form [:message] nil nil))
            (is (= "Message" (:label (test.dom/attrs input))))))))))

(defn run-tests []
  (t/run-tests))
