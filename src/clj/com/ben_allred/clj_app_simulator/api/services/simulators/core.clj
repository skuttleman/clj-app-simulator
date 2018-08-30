(ns com.ben-allred.clj-app-simulator.api.services.simulators.core
  (:refer-clojure :exclude [set!])
  (:require [com.ben-allred.clj-app-simulator.api.services.simulators.http :as http.sim]
            [com.ben-allred.clj-app-simulator.api.services.simulators.ws :as ws.sim]
            [com.ben-allred.clj-app-simulator.api.services.simulators.file :as file.sim]
            [com.ben-allred.clj-app-simulator.api.services.simulators.simulators :as sims]
            [compojure.core :as c]
            [com.ben-allred.clj-app-simulator.api.services.simulators.common :as common]
            [com.ben-allred.clj-app-simulator.api.services.activity :as activity]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.utils.uuids :as uuids]))

(def ^:private validators [#'http.sim/valid?
                           #'ws.sim/valid?
                           #'file.sim/valid?])

(def ^:private sim-fns [#'http.sim/->HttpSimulator
                        #'ws.sim/->WsSimulator
                        #'file.sim/->FileSimulator])

(defn ^:private simulator-configs
  ([]
   (sims/simulators))
  ([f]
   (map f (sims/simulators))))

(defn valid? [config]
  (some #(% config) validators))

(defn config->?simulator [config]
  (let [id (uuids/random)]
    (->> sim-fns
         (keep #(% id config))
         (first))))

(defn make-simulator! [config]
  (when-let [simulator (config->?simulator config)]
    (sims/add! simulator)))

(defn details []
  (->> (simulator-configs common/details)
       (assoc {} :simulators)
       (conj [:ok])))

(defn add [config]
  (if-let [simulator (make-simulator! config)]
    (let [sim (common/details simulator)]
      (activity/publish :simulators/add sim)
      [:created {:simulator sim}])
    [:bad-request {:message "error creating simulator"}]))

(defn set! [configs]
  (let [invalid-configs (remove valid? configs)]
    (if (empty? invalid-configs)
      (do
        (sims/clear!)
        (let [sims (->> configs
                        (map make-simulator!)
                        (map common/details))]
          (activity/publish :simulators/init sims)
          [:created {:simulators sims}]))
      [:bad-request {:message "one or more invalid simulators"}])))

(defn reset-all! []
  (let [sims (simulator-configs)]
    (dorun (map common/reset sims))
    (activity/publish :simulators/reset-all (map common/details sims)))
  [:no-content])

(defn routes []
  (->> (simulator-configs common/routes)
       (mapcat identity)
       (apply c/routes)))

(comment
  (com.ben-allred.clj-app-simulator.api.services.simulators.core/set! [{:method :http/post :path "/stepped" :response {:status 201}}])
  (require '[bote.core :as smtp])
  (def smtp-server (smtp/create-smtp-server #(log/spy %)
                                       :port 2525
                                       :enable-tls? true))
  (.start smtp-server)
  (.stop smtp-server))
