(ns com.ben-allred.clj-app-simulator.api.server
  (:gen-class)
  (:require
    [clojure.spec.alpha :as s]
    [clojure.tools.nrepl.server :as nrepl]
    [com.ben-allred.clj-app-simulator.api.services.middleware :as middleware]
    [com.ben-allred.clj-app-simulator.api.services.resources.core :as resources]
    [com.ben-allred.clj-app-simulator.api.services.routes :as routes]
    [com.ben-allred.clj-app-simulator.api.utils.respond :as respond]
    [com.ben-allred.clj-app-simulator.services.env :as env]
    [com.ben-allred.clj-app-simulator.utils.logging :as log]
    [compojure.core :refer [ANY DELETE GET POST PUT context defroutes]]
    [compojure.handler :refer [site]]
    [compojure.response :refer [Renderable]]
    [immutant.web :as web]
    [ring.middleware.multipart-params.temp-file :as temp-file]
    [ring.middleware.reload :refer [wrap-reload]])
  (:import
    (clojure.lang IPersistentVector)))

(extend-protocol Renderable
  IPersistentVector
  (render [this _]
    (respond/with this)))

(def ^:private app
  (-> #'routes/base
      (site {:multipart {:store (temp-file/temp-file-store {:expires-in nil})}})
      (middleware/content-type)
      (middleware/log-response)))

(defn ^:private server-port [env key fallback]
  (let [port (str (or (get env key) (env/get key) fallback))]
    (Integer/parseInt port)))

(defn ^:private run [app env]
  (let [port (server-port env :port 3000)
        server (web/run app {:port port})
        runtime (Runtime/getRuntime)]
    (->> ^Runnable (fn [] (resources/clear! (env/env*)))
         (Thread.)
         (.addShutdownHook runtime))
    (println "Server is listening on port" port)
    server))

(def ^:private -dev-server nil)

(def ^:private -dev-repl-server nil)

(defn -main [& {:as env}]
  [(partial web/stop (run app env))])

(defn -dev [& {:as env}]
  (alter-var-root #'env/get assoc :dev? true)
  (s/check-asserts true)
  (let [server (run #'app env)
        nrepl-port (server-port env :nrepl-port 7000)
        repl-server (nrepl/start-server :port nrepl-port)]
    (println "Server is running with #'wrap-reload")
    (println "REPL is listening on port" nrepl-port)
    (alter-var-root #'-dev-server (constantly server))
    (alter-var-root #'-dev-repl-server (constantly repl-server))
    [(partial web/stop server) (partial nrepl/stop-server repl-server)]))
