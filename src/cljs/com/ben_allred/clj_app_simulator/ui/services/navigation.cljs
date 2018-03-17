(ns com.ben-allred.clj-app-simulator.ui.services.navigation
    (:require [bidi.bidi :as bidi]
              [clojure.string :as string]
              [com.ben-allred.clj-app-simulator.utils.keywords :as keywords]
              [com.ben-allred.clj-app-simulator.utils.query-params :as qp]
              [pushy.core :as pushy]
              [com.ben-allred.clj-app-simulator.ui.services.store.core :as store]))

(defn ^:private namify [[k v]]
    [k (str (keywords/safe-name v))])

(def ^:private routes
    ["/"
     [["" :home]
      ["login" :login]
      ["logout" :logout]
      ["repos"
       [["" :repos]
        [["/" [#"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}" :repo-id]] :repo]]]
      [true :not-found]]])

(defn match-route [path]
    (let [qp (qp/parse (second (string/split path #"\?")))]
        (cond->
            (bidi/match-route routes path)
            (seq qp) (assoc :query-params qp))))

(defn path-for
    ([page] (path-for page nil))
    ([page {:keys [query-params] :as params}]
                (let [qp (qp/stringify query-params)]
                    (cond-> (apply bidi/path-for routes page (mapcat namify params))
                        (seq qp) (str "?" qp)))))

(defonce history
    (let [history (pushy/pushy (comp store/dispatch (partial conj [:router/navigate])) match-route)]
        (pushy/start! history)
        history))

(defn reload! []
    (.reload (.-location js/window)))

(defn navigate!
    ([page] (navigate! page nil))
    ([page params]
        (pushy/set-token! history (path-for page params))))

(defn go-to! [path]
    (set! (.-pathname (.-location js/window)) path))

(defn nav-and-replace!
    ([page] (nav-and-replace! page nil))
    ([page params]
        (pushy/replace-token! history (path-for page params))))
