(ns com.ben-allred.clj-app-simulator.api.server
    (:gen-class)
    (:use compojure.core org.httpkit.server)
    (:require [compojure.handler :refer [site]]
              [compojure.route :as route]
              [ring.middleware.reload :refer [wrap-reload]]
              [clojure.tools.nrepl.server :as nrepl]
              [com.ben-allred.clj-app-simulator.api.services.middleware :as middleware]
              [ring.util.response :as response]
              [com.ben-allred.clj-app-simulator.api.services.simulators.core :as simulators]
              [com.ben-allred.clj-app-simulator.api.services.activity :as activity]
              [com.ben-allred.clj-app-simulator.api.utils.respond :as respond]
              [com.ben-allred.clj-app-simulator.services.env :as env]))

(defroutes ^:private base
    (context "/api/simulators" []
        (GET "/" [] (simulators/configs))
        (POST "/" request (simulators/add (get-in request [:body :simulator])))
        (POST "/init" request (simulators/set! (get-in request [:body :simulators])))
        (DELETE "/reset" [] (simulators/reset-all!))
        (GET "/activity" request (activity/sub request)))
    (context "/" []
        (simulators/routes)
        (ANY "/simulators/*" [] (respond/with [:not-implemented {:message "simulator not found"}]))
        (GET "/health" [] (respond/with [:ok {:a :ok}]))
        (route/resources "/")
        (GET "/*" [] (response/resource-response "index.html" {:root "public"}))
        (ANY "/*" [] (respond/with [:not-found]))))

(def ^:private app
    (-> #'base
        (middleware/log-response)
        (middleware/content-type)
        (site)))

(defn ^:private server-port [key env fallback]
    (let [port (str (or (get env key) (env/get key) fallback))]
        (Integer/parseInt port)))

(defn ^:private run [app env]
    (let [port (server-port :port env 3000)
          stop (run-server #'app {:port port})]
        (println "Server is listening on port" port)
        stop))

(defn -main [& {:as env}]
    [(run app env)])

(defn -dev [& {:as env}]
    (let [stop-server (run (wrap-reload #'app) env)
          nrepl-port (server-port :nrepl-port env 7000)
          stop-repl   (nrepl/start-server :port nrepl-port)]
        (println "Server is running with #'wrap-reload")
        (println "REPL is listening on port" nrepl-port)
        [stop-server stop-repl]))
