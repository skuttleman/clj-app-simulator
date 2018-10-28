(ns com.ben-allred.app-simulator.api.services.routes
  (:require
    [com.ben-allred.app-simulator.api.services.activity :as activity]
    [com.ben-allred.app-simulator.api.services.html :as html]
    [com.ben-allred.app-simulator.api.services.resources.core :as resources]
    [com.ben-allred.app-simulator.api.services.simulators.core :as simulators]
    [com.ben-allred.app-simulator.services.env :as env]
    [com.ben-allred.app-simulator.utils.colls :as colls]
    [compojure.core :refer [ANY DELETE GET POST PUT context defroutes]]
    [compojure.route :as route]))

(defroutes ^:private sim-api
  (context "/simulators" []
    (GET "/" [] (simulators/details (env/env*)))
    (POST "/" request (simulators/add (env/env*) (get-in request [:body :simulator])))
    (POST "/init" request (simulators/set! (env/env*) (get-in request [:body :simulators])))
    (DELETE "/reset" [] (simulators/reset-all! (env/env*)))))

(def ^:private res-api
  (context "/resources" []
    (POST "/" request
      (->> (get-in request [:params :files])
           (colls/force-sequential)
           (resources/upload! (env/env*))
           (assoc {} :resources)
           (conj [:created])))
    (PUT "/:resource-id" request
      (let [{:keys [resource-id file]} (:params request)]
        (->> file
             (resources/upload! (env/env*) resource-id)
             (assoc {} :resource)
             (conj [:ok]))))
    (GET "/" []
      [:ok {:resources (resources/list-files (env/env*))}])
    (DELETE "/" []
      (resources/clear! (env/env*))
      [:no-content])
    (DELETE "/:resource-id" [resource-id]
      (resources/remove! (env/env*) resource-id)
      [:no-content])))

(defroutes ^:private sims
  (context "/" []
    (simulators/routes (env/env*))
    (context "/simulators" []
      (ANY "/" [] [:not-found {:message "simulator not found"}])
      (ANY "/*" [] [:not-found {:message "simulator not found"}]))
    (ANY "/api/*" [] [:not-found])))

(defroutes ^:private web
  (context "/" []
    (route/resources "/")
    (GET "/health" [] [:ok {:a :ok}])
    (GET "/*" req [:ok
                   (-> req
                       (select-keys #{:uri :query-string})
                       (html/render (env/env*)))
                   {"content-type" "text/html"}])))

(defroutes base
  (context "/api" []
    sim-api
    (GET "/simulators/activity" request (activity/sub (env/env*) request))
    res-api)
  (context "/" []
    sims
    web
    (ANY "/*" [] [:not-found])))
