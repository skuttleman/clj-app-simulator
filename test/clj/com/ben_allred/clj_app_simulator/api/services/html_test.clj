(ns com.ben-allred.clj-app-simulator.api.services.html-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.clj-app-simulator.api.services.html :as html]
            [test.utils.dom :as test.dom]
            [test.utils.spies :as spies]
            [com.ben-allred.clj-app-simulator.templates.core :as templates]
            [com.ben-allred.clj-app-simulator.templates.views.main :as main]
            [com.ben-allred.clj-app-simulator.utils.uuids :as uuids]
            [com.ben-allred.clj-app-simulator.services.ui-reducers :as ui-reducers]))

(deftest ^:unit tree->html-test
  (testing "(tree->html)"
    (let [hiccup-spy (spies/constantly "[HTML]")]
      (with-redefs [html/hiccup hiccup-spy]
        (let [html (html/tree->html ::tree)]
          (testing "converts tree to html"
            (is (spies/called-with? hiccup-spy ::tree)))

          (testing "prepends html DOCTYPE"
            (is (= "<!DOCTYPE html>[HTML]" html))))))))

(deftest ^:unit build-tree-test
  (testing "(build-tree)"
    (testing "when building the template"
      (let [tree (html/build-tree nil)]
        (testing "links to main.css"
          (let [attrs (->> (test.dom/query-all tree :link)
                           (map test.dom/attrs)
                           (filter (comp #{"/css/main.css"} :href))
                           (first))]
            (is (= "text/css" (:type attrs)))
            (is (= "stylesheet" (:rel attrs)))))

        (testing "uses app.js"
          (let [attrs (->> (test.dom/query-all tree :script)
                           (map test.dom/attrs)
                           (filter (comp #{"/js/compiled/app.js"} :src))
                           (first))]
            (is (= "text/javascript" (:type attrs)))))

        (testing "mounts the app"
          (is (->> (test.dom/query-all tree :script)
                   (filter #(test.dom/re-contains? % #"com\.ben_allred\.clj_app_simulator\.ui\.app\.mount_BANG_\(\)"))
                   (first))))))

    (testing "when including content"
      (let [render-spy (spies/constantly ::rendered)]
        (with-redefs [templates/render render-spy]
          (let [tree (html/build-tree ::content)]
            (testing "renders content"
              (is (spies/called-with? render-spy ::content)))

            (testing "embeds content in #app"
              (is (-> tree
                      (test.dom/query-one :#app)
                      (test.dom/contains? ::rendered))))))))))

(deftest ^:unit hydrate-test
  (testing "(hydrate)"
    (let [build-spy (spies/constantly ::tree)
          html-spy (spies/constantly ::html)
          reducer-spy (spies/constantly {:some :state})]
      (with-redefs [html/build-tree build-spy
                    html/tree->html html-spy
                    ui-reducers/root reducer-spy]
        (let [html (html/hydrate ::page)
              [root attrs] (ffirst (spies/calls build-spy))]
          (testing "gets the default state"
            (is (spies/called-with? reducer-spy)))

          (testing "builds the dom tree"
            (is (= main/app root))
            (is (= ::page (:page attrs)))
            (is (= :state (:some attrs))))

          (testing "converts the tree to html"
            (is (spies/called-with? html-spy ::tree))
            (is (= ::html html))))))))

(deftest ^:unit render-test
  (testing "(render)"
    (let [hydrate-spy (spies/constantly ::html)]
      (with-redefs [html/hydrate hydrate-spy]
        (testing "returns html"
          (is (= ::html (html/render {:uri ""}))))

        (testing "when requesting /details"
          (spies/reset! hydrate-spy)
          (let [id (str (uuids/random))]
            (html/render {:uri (str "/details/" id)})

            (is (spies/called-with? hydrate-spy {:handler :details :route-params {:id id}}))))

        (testing "when requesting /resources"
          (spies/reset! hydrate-spy)
          (html/render {:uri "/resources"})

          (is (spies/called-with? hydrate-spy {:handler :resources})))

        (testing "when requesting /create"
          (spies/reset! hydrate-spy)
          (html/render {:uri "/create" :params {:type ::some-type}})

          (is (spies/called-with? hydrate-spy {:handler :new :query-params {:type ::some-type}})))

        (testing "when requesting /"
          (spies/reset! hydrate-spy)
          (html/render {:uri "/"})

          (is (spies/called-with? hydrate-spy {:handler :home})))

        (testing "when requesting any other path"
          (spies/reset! hydrate-spy)
          (html/render {:uri "/any/old/path"})

          (is (spies/called-with? hydrate-spy {:handler :not-found})))))))

(comment
  (clojure.test/run-tests))
