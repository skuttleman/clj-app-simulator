(ns com.ben-allred.clj-app-simulator.api.server
  (:gen-class)
  (:require
    [clojure.tools.nrepl.server :as nrepl]
    [com.ben-allred.clj-app-simulator.api.services.activity :as activity]
    [com.ben-allred.clj-app-simulator.api.services.html :as html]
    [com.ben-allred.clj-app-simulator.api.services.middleware :as middleware]
    [com.ben-allred.clj-app-simulator.api.services.resources.core :as resources]
    [com.ben-allred.clj-app-simulator.api.services.simulators.core :as simulators]
    [com.ben-allred.clj-app-simulator.api.utils.respond :as respond]
    [com.ben-allred.clj-app-simulator.services.env :as env]
    [com.ben-allred.clj-app-simulator.utils.colls :as colls]
    [com.ben-allred.clj-app-simulator.utils.logging :as log]
    [compojure.core :refer [ANY DELETE GET POST PUT context defroutes]]
    [compojure.handler :refer [site]]
    [compojure.response :refer [Renderable]]
    [compojure.route :as route]
    [immutant.web :as web]
    [ring.middleware.multipart-params.temp-file :as temp-file]
    [ring.middleware.reload :refer [wrap-reload]])
  (:import
    (clojure.lang IPersistentVector)))

(extend-protocol Renderable
  IPersistentVector
  (render [this _]
    (respond/with this)))

(defn ^:private env* []
  (keyword (env/get :ring-env :app)))

(defroutes ^:private base
  (context "/api" []
    (context "/simulators" []
      (GET "/" [] (simulators/details (env*)))
      (POST "/" request (simulators/add (env*) (get-in request [:body :simulator])))
      (POST "/init" request (simulators/set! (env*) (get-in request [:body :simulators])))
      (DELETE "/reset" [] (simulators/reset-all! (env*)))
      (GET "/activity" request (activity/sub (env*) request)))
    (context "/resources" []
      (POST "/" request
        (->> (get-in request [:params :files])
             (colls/force-sequential)
             (resources/upload! (env*))
             (conj [:created])))
      (PUT "/:resource-id" request
        (let [{:keys [resource-id file]} (:params request)]
          (->> file
               (resources/upload! (env*) resource-id)
               (conj [:ok]))))
      (GET "/" []
        [:ok {:uploads (resources/list-files (env*))}])
      (DELETE "/" []
        (resources/clear! (env*))
        [:no-content])
      (DELETE "/:resource-id" [resource-id]
        (resources/remove! (env*) resource-id)
        [:no-content])))
  (context "/" []
    (simulators/routes (env*))
    (context "/simulators" []
      (ANY "/" [] [:not-found {:message "simulator not found"}])
      (ANY "/*" [] [:not-found {:message "simulator not found"}]))
    (ANY "/api/*" [] [:not-found])
    (route/resources "/")
    (GET "/health" [] [:ok {:a :ok}])
    (GET "/*" req [:ok
                   (-> req
                       (select-keys #{:uri :query-string})
                       (html/render (env*)))
                   {"content-type" "text/html"}])
    (ANY "/*" [] [:not-found])))

(def ^:private app
  (-> #'base
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
    (->> ^Runnable (fn [] (resources/clear! (env*)))
         (Thread.)
         (.addShutdownHook runtime))
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
