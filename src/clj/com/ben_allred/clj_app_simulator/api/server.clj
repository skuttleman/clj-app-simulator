(ns com.ben-allred.clj-app-simulator.api.server
    (:gen-class)
    (:use compojure.core org.httpkit.server)
    (:require [compojure.handler :refer [site]]
              [compojure.route :as route]
              [ring.middleware.reload :refer [wrap-reload]]
              [ring.middleware.json :as mw.json]
              [clojure.tools.nrepl.server :as nrepl]
              [com.ben-allred.clj-app-simulator.api.services.middleware :as middleware]
              [ring.util.response :as response]
              [com.ben-allred.clj-app-simulator.services.env :as env]
              [com.ben-allred.clj-app-simulator.utils.logging :as log]))

(defroutes ^:private base
    (GET "/health" [] {:status 200 :body {:a :ok}})
    (route/resources "/")
    (GET "/*" [] (response/resource-response "index.html" {:root "public"}))
    (ANY "/*" [] {:status 404}))

(def ^:private app
    (-> #'base
        (middleware/log-response)
        (middleware/content-type)
        (site)))

(def ^:private server-port
    (if-let [port (env/get :port)]
        (Integer/parseInt (str port))
        3000))

(defn ^:private run [app]
    (run-server #'app {:port server-port})
    (println "Server is listening on port" server-port))

(defn -main [& args]
    (run app))

(defn -dev [& args]
    (println "Server is running with #'wrap-reload")
    (run (wrap-reload #'app))
    (nrepl/start-server :port 7000)
    (println "REPL is listening on port" 7000))
