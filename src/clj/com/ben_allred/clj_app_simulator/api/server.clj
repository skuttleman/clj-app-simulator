(ns com.ben-allred.clj-app-simulator.api.server
  (:gen-class)
  (:use compojure.core)
  (:require [compojure.handler :refer [site]]
            [immutant.web :as web]
            [compojure.route :as route]
            [ring.middleware.reload :refer [wrap-reload]]
            [clojure.tools.nrepl.server :as nrepl]
            [com.ben-allred.clj-app-simulator.api.services.middleware :as middleware]
            [ring.util.response :as response]
            [com.ben-allred.clj-app-simulator.api.services.resources.core :as resources]
            [com.ben-allred.clj-app-simulator.api.services.simulators.core :as simulators]
            [com.ben-allred.clj-app-simulator.api.services.activity :as activity]
            [com.ben-allred.clj-app-simulator.api.utils.respond :as respond]
            [com.ben-allred.clj-app-simulator.utils.colls :as colls]
            [com.ben-allred.clj-app-simulator.services.env :as env]
            [com.ben-allred.clj-app-simulator.utils.logging :as log]
            [com.ben-allred.clj-app-simulator.utils.uuids :as uuids]))

(defn ^:private not-found
  ([] (not-found nil))
  ([message]
   (respond/with [:not-found (when message {:message message})])))

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
             (conj [:created])
             (respond/with)))
      (GET "/" []
        (respond/with [:ok {:uploads (resources/list-files)}]))
      (DELETE "/" []
        (resources/clear!)
        (respond/with [:no-content]))
      (DELETE "/:resource-id" [resource-id]
        (resources/remove! (uuids/->uuid resource-id))
        (respond/with [:no-content]))))
  (context "/" []
    (simulators/routes)
    (context "/simulators" []
      (ANY "/" [] (not-found "simulator not found"))
      (ANY "/*" [] (not-found "simulator not found")))
    (route/resources "/")
    (GET "/*" [] (response/resource-response "index.html" {:root "public"}))
    (ANY "/*" [] (not-found))))

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
