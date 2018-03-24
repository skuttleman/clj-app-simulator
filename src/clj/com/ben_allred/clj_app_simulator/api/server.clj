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
        (DELETE "/reset" request (simulators/reset-all!))
        (GET "/activity" request (activity/sub request)))
    (context "/" []
        (simulators/routes)
        (GET "/health" [] (respond/with [:ok {:a :ok}]))
        (route/resources "/")
        (GET "/*" [] (response/resource-response "index.html" {:root "public"}))
        (ANY "/*" [] (respond/with [:not-found]))))

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
