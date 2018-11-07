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
    (GET "/" []
      (->> (env/env*)
           (simulators/details)
           (hash-map :simulators)
           (conj [:http.status/ok])))
    (POST "/" request
      (->> (get-in request [:body :simulator])
           (simulators/add (env/env*))
           (hash-map :simulator)
           (conj [:http.status/created])))
    (POST "/init" request
      (->> (get-in request [:body :simulators])
           (simulators/set! (env/env*))
           (hash-map :simulators)
           (conj [:http.status/created])))
    (DELETE "/reset" []
      (simulators/reset-all! (env/env*))
      [:http.status/no-content])))

(def ^:private res-api
  (context "/resources" []
    (POST "/" request
      (->> (get-in request [:params :files])
           (colls/force-sequential)
           (resources/upload! (env/env*))
           (hash-map :resources)
           (conj [:http.status/created])))
    (PUT "/:resource-id" request
      (let [{:keys [resource-id file]} (:params request)]
        (->> file
             (resources/upload! (env/env*) resource-id)
             (hash-map :resource)
             (conj [:http.status/ok]))))
    (GET "/" []
      (->> (resources/list-files (env/env*))
           (hash-map :resources)
           (conj [:http.status/ok])))
    (DELETE "/" []
      (resources/clear! (env/env*))
      [:http.status/no-content])
    (DELETE "/:resource-id" [resource-id]
      (resources/remove! (env/env*) resource-id)
      [:http.status/no-content])))

(defroutes ^:private sims
  (context "/" []
    (simulators/routes (env/env*))
    (context "/simulators" []
      (ANY "/*" [] [:http.status/not-found {:message "simulator not found"}]))
    (context "/api" []
      (ANY "/*" [] [:http.status/not-found]))))

(defroutes ^:private web
  (context "/" []
    (route/resources "/")
    (GET "/health" [] [:http.status/ok {:a :ok}])
    (GET "/*" req [:http.status/ok
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
    (ANY "/*" [] [:http.status/not-found])))
