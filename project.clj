(defproject com.ben-allred/app-simulator "0.1.0"
  :description "Application simulator"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :main com.ben-allred.app-simulator.api.server
  :aot [com.ben-allred.app-simulator.api.server]
  :min-lein-version "2.6.1"

  :dependencies [[bidi "2.1.3"]
                 [clj-http "3.9.1"]
                 [cljs-http "0.1.45"]
                 [cljsjs/moment "2.10.6-0"]
                 [com.ben-allred/collaj "0.8.0"]
                 [com.ben-allred/formation "0.4.1"]
                 [com.cognitect/transit-clj "0.8.300"]
                 [com.cognitect/transit-cljs "0.8.243"]
                 [com.taoensso/timbre "4.10.0"]
                 [compojure "1.6.0"]
                 [environ "1.1.0"]
                 [hiccup "1.0.5"]
                 [io.nervous/kvlt "0.1.4"]
                 [kibu/pushy "0.3.8"]
                 [metosin/jsonista "0.1.1"]
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [org.clojure/core.async "0.3.465"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/spec.alpha "0.2.176"]
                 [org.clojure/test.check "0.9.0"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [org.immutant/immutant "2.1.10"]
                 [reagent "0.7.0"]
                 [ring/ring-core "1.3.2"]
                 [ring/ring-defaults "0.2.1"]
                 [ring/ring-devel "1.6.3"]
                 [ring/ring-json "0.3.1"]
                 [stylefruits/gniazdo "1.0.1"]]

  :plugins [[com.jakemccrary/lein-test-refresh "0.23.0"]
            [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]
            [lein-cooper "1.2.2"]
            [lein-figwheel "0.5.14"]
            [lein-sass "0.5.0"]]

  :jar-name "app-simulator.jar"
  :uberjar-name "app-simulator-standalone.jar"
  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :test-paths ["test/clj" "test/cljs" "test/cljc" "test/api" "test/common"]
  :test-selectors {:focused     :focused
                   :integration :integration
                   :unit        :unit}

  :cljsbuild {:builds [{:id           "dev"
                        :source-paths ["src/cljs" "src/cljc" "test/cljs" "test/cljc" "test/common"]
                        :figwheel     {:on-jsload "com.ben-allred.app-simulator.ui.tests/on-reload"}
                        :compiler     {:main                 com.ben-allred.app-simulator.ui.tests
                                       :asset-path           "/js/compiled/out"
                                       :output-to            "resources/public/js/compiled/app.js"
                                       :output-dir           "resources/public/js/compiled/out"
                                       :source-map-timestamp true
                                       :preloads             [devtools.preload]}}
                       {:id           "min"
                        :source-paths ["src/cljs" "src/cljc"]
                        :jar          true
                        :compiler     {:output-to     "resources/public/js/compiled/app.js"
                                       :main          com.ben-allred.app-simulator.ui.app
                                       :optimizations :advanced
                                       :pretty-print  false
                                       :language-in   :ecmascript6
                                       :language-out  :ecmascript5}}]}
  :figwheel {:css-dirs   ["resources/public/css"]
             :nrepl-port 7888}
  :sass {:src              "src/scss"
         :output-directory "resources/public/css/"}

  :cooper {"cljs"   ["lein" "figwheel"]
           "sass"   ["lein" "sass" "auto"]
           "server" ["lein" "run"]}

  :profiles {:dev     {:dependencies  [[binaryage/devtools "0.9.4"]
                                       [figwheel-sidecar "0.5.14"]
                                       [com.cemerick/piggieback "0.2.2"]]
                       :main          com.ben-allred.app-simulator.api.server/-dev
                       :source-paths  ["src/clj" "src/cljs" "src/cljc" "dev"]
                       :plugins       [[cider/cider-nrepl "0.16.0"]]
                       :clean-targets ^{:protect false :replace true} ["resources/public/js"
                                                                       "resources/public/css"
                                                                       :target-path]
                       :repl-options  {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}
             :uberjar {:clean-targets ^{:protect false :replace true} ["resources/public/js"
                                                                       "resources/public/css"
                                                                       :target-path]
                       :sass          {:style :compressed}
                       :prep-tasks    ["compile" ["cljsbuild" "once" "min"] ["sass" "once"]]}}
  :repl-options {:init-ns integration.tests.simulators.http})
