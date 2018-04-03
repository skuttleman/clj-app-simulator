(ns com.ben-allred.clj-app-simulator.ui.views.main
  (:require [com.ben-allred.clj-app-simulator.ui.services.navigation :as nav]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.ui.views.simulators :as sims]
            [reagent.core :as r]
            [com.ben-allred.clj-app-simulator.ui.services.store.actions :as actions]
            [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]))

#_(defn sim-form []
  (let [edn (r/atom "")]
    (fn []
      [:form {:on-submit #(do (.preventDefault %)
                              (-> @edn
                                  (cljs.tools.reader/read-string)
                                  (actions/create-simulator)
                                  (store/dispatch)))}
       [:div [:textarea {:on-change #(reset! edn (.-value (.-target %)))
                         :value     @edn}]]
       [:button.button.button-primary.pure-button
        "Submit"]])))

(defn header []
  [:header.header
   [:a.home-link {:href (nav/path-for :home)}
    [:img.logo {:src "/images/logo.png"}]
    [:h1 "App Simulator"]]])

(defn root [{:keys [simulators]}]
  [:div
   [:h2 "Simulators"]
   [sims/simulators simulators]
   #_[sim-form]])
