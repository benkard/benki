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
  ;; NB. response/redirect calls options/resolve-uri.
  (response/redirect "/wiki/Home"))

(defpage "/wiki/:title" {title :title, revision-id :revision}
  (let [revisions-with-title (-> page_revisions
                                 (select (where (=* :title title)))
                                 (sort [:date#desc]))
        revision             (if revision-id
                               (with-dbt (first @(select page_revisions
                                                         (where (=* :id (Integer/parseInt revision-id))))))
                               (with-dbt (first @revisions-with-title)))]
    ;; FIXME: Insert WikiLinks.
    (layout (fmt nil "~A — Benki~@[/~A~] " title revision-id)
            (if revision
              [:div#wiki-page-content (:content revision)]
              [:div#wiki-page-content [:p "This page does not exist yet."]])
            [:hr]
            [:div#wiki-page-footer {:style "text-align: right"}
             [:a {:href (link :wiki title "/revisions")} "Page revisions"
              ]])))

(defpage "/wiki/:title/revisions" {title :title}
  (let [;; page      (-> page_revisions
        ;;               (select (where (=* :title "abc")))
        ;;               (project [:page])
        ;;               (join pages (where (=* :id :page)))
        ;;               (sort [:date#desc])
        ;;               (project [:id])
        ;;               (limit 1))
        ;; revisions (-> page
        ;;               (rename {:id :page_id})
        ;;               (join page_revisions (where (=* :page_id :id))))
        revisions (with-dbt
                    (query "SELECT r.*
                              FROM wiki_page_revisions r
                              JOIN (SELECT * FROM wiki_page_revisions
                                            WHERE title = ?
                                            ORDER BY date DESC
                                            LIMIT 1) pr
                                ON (pr.page = r.page)
                             ORDER BY date DESC"
                         "Home"))]
    (with-dbt
      (layout (fmt nil "Revision list — ~A — Benki" title)
        [:table {:style ""}
         [:thead
          [:th "Date"]
          [:th "Title"]]
         [:tbody
          (for [rev revisions]
            [:tr
             [:td [:a {:href (link :wiki
                                   (:title rev)
                                   (fmt nil "?revision=~a" (:id rev)))}
                   (:date rev)]]
             [:td (:title rev)]])]]))))

(defn insert-empty-page []
  (sql/with-query-results results ["INSERT INTO wiki_pages DEFAULT VALUES RETURNING *"]
    (first (into () results))))

(defpage [:post "/wiki/:title"] {title :title, content :content}
  (with-dbt
    (let [revisions (-> page_revisions
                        (select (where (=* :title title)))
                        (sort [:date#desc]))
          revision  (first @revisions)
          page      (:page revision)]
      ;; FIXME: Strip auto-generated WikiLinks from input.
      (if-let [user (Integer. (session/get :user))]
        (let [page-id (if page page (:id (insert-empty-page)))]
          (sql/insert-values
           :wiki_page_revisions
           [:page   :title :content :author :format]
           [page-id title  content  user    "html5"])
          {:stetus 200, :headers {}, :body ""})
        {:status 403, :headers {}, :body ""}))))
