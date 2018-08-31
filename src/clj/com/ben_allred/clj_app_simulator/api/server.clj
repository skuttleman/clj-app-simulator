(ns com.ben-allred.clj-app-simulator.api.server
  (:gen-class)
  (:use compojure.core)
  (:require [clojure.tools.nrepl.server :as nrepl]
            [com.ben-allred.clj-app-simulator.api.services.activity :as activity]
            [com.ben-allred.clj-app-simulator.api.services.html :as html]
            [com.ben-allred.clj-app-simulator.api.services.middleware :as middleware]
            [com.ben-allred.clj-app-simulator.api.services.resources.core :as resources]
            [com.ben-allred.clj-app-simulator.api.services.simulators.core :as simulators]
            [com.ben-allred.clj-app-simulator.api.utils.respond :as respond]
            [com.ben-allred.clj-app-simulator.services.env :as env]
            [com.ben-allred.clj-app-simulator.utils.colls :as colls]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.utils.uuids :as uuids]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [immutant.web :as web]
            [ring.middleware.reload :refer [wrap-reload]])
  (:import (clojure.lang IPersistentVector)))

(extend-protocol compojure.response/Renderable
  IPersistentVector
  (render [this _]
    (respond/with this)))

(defroutes ^:private base
  (context "/api" []
    (context "/simulators" []
      (GET "/" [] (simulators/details))
      (POST "/" request (simulators/add (get-in request [:body :simulator])))
      (POST "/init" request (simulators/set! (get-in request [:body :simulators])))
      (DELETE "/reset" [] (simulators/reset-all!))
      (GET "/activity" request (activity/sub request)))
    (context "/resources" []
      (POST "/" request
        (->> (get-in request [:params :files])
             (colls/force-sequential)
             (resources/upload!)
             (conj [:created])))
      (PUT "/:resource-id" request
        (let [{:keys [resource-id file]} (:params request)]
          (->> file
               (resources/upload! resource-id)
               (conj [:ok]))))
      (GET "/" []
        [:ok {:uploads (resources/list-files)}])
      (DELETE "/" []
        (resources/clear!)
        [:no-content])
      (DELETE "/:resource-id" [resource-id]
        (resources/remove! (uuids/->uuid resource-id))
        [:no-content])))
  (context "/" []
    (simulators/routes)
    (context "/simulators" []
      (ANY "/" [] [:not-found {:message "simulator not found"}])
      (ANY "/*" [] [:not-found {:message "simulator not found"}]))
    (ANY "/api/*" [] [:not-found])
    (route/resources "/")
    (GET "/health" [] [:ok {:a :ok}])
    (GET "/*" req [:ok
                   (-> req
                       (select-keys #{:uri :params})
                       (html/render))
                   {"content-type" "text/html"}])
    (ANY "/*" [] [:not-found])))

(def ^:private app
  (-> #'base
      (site)
      (middleware/content-type)
      (middleware/log-response)))

(defn ^:private server-port [env key fallback]
  (let [port (str (or (get env key) (env/get key) fallback))]
    (Integer/parseInt port)))

(defn ^:private run [app env]
  (let [port (server-port env :port 3000)
        server (web/run app {:port port})]
    (println "Server is listening on port" port)
    server))

(def ^:private -dev-server nil)

(def ^:private -dev-repl-server nil)

(defn -main [& {:as env}]
  [(partial web/stop (run app env))])

(defn -dev [& {:as env}]
  (let [server (run #'app env)
        nrepl-port (server-port env :nrepl-port 7000)
        repl-server (nrepl/start-server :port nrepl-port)]
    (println "Server is running with #'wrap-reload")
    (println "REPL is listening on port" nrepl-port)
    (alter-var-root #'-dev-server (constantly server))
    (alter-var-root #'-dev-repl-server (constantly repl-server))
    [(partial web/stop server) (partial nrepl/stop-server repl-server)]))
