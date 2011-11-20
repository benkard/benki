(ns mulk.benki.wiki
  (:refer-clojure :exclude [distinct conj! case compile drop take sort disj!
                            resultset-seq])
  (:use [clojure         repl pprint]
        [clojure.contrib error-kit]
        [hiccup core     page-helpers]
        [mulk.benki      util db]
        [clojure.core.match.core
         :only [match]]
        [clojureql core predicates]
        noir.core)
  (:require [noir.session      :as session]
            [noir.response     :as response]
            [clojure.java.jdbc :as sql]))


(def page_revisions (table :wiki_page_revisions))
(def pages          (table :wiki_pages))


(defpage "/wiki" []
  (response/redirect (resolve-uri "/wiki/Home")))

(defpage "/wiki/:title" {title :title, revision-id :revision}
  (let [revisions (-> page_revisions
                      (select (where (=* :title title)))
                      (select (where (if revision-id
                                       (=* :id revision-id)
                                       (=* 0 0))))
                      (sort [:date#desc]))
        revision  (with-dbt (first @revisions))]
    (layout (fmt nil "~A — Benki~@[/~A~] " title revision-id)
      (if revision
        [:div#wiki-page-content (:content revision)]
        [:div#wiki-page-content [:p "This page does not exist yet."]])
      [:hr]
      [:div#wiki-page-footer {:style "text-align: right"}
       [:a {:href (link :wiki title :revisions)} "Page revisions"
        ]])))

(defn insert-empty-page []
  (sql/with-query-results results ["insert into wiki_pages default values returning *"]
    (first (into () results))))

(defpage [:post "/wiki/:title"] {title :title, content :content}
  (with-dbt
    (let [revisions (-> page_revisions
                        (select (where (=* :title title)))
                        (sort [:date#desc]))
          revision  (first @revisions)
          page      (:page revision)]
      (println "For page: " title " (id " page ");  got content: " content)
      (if-let [user (Integer. (session/get :user))]
        (let [page-id (if page page (:id (insert-empty-page)))]
          (sql/insert-values
           :wiki_page_revisions
           [:page   :title :content :author :format]
           [page-id title  content  user    "html5"])
          {:stetus 200, :headers {}, :body ""})
        {:status 403, :headers {}, :body ""}))))
