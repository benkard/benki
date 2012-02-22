(ns mulk.benki.wiki
  (:refer-clojure :exclude [distinct conj! case compile drop take sort disj!
                            resultset-seq])
  (:use [clojure         repl pprint]
        [hiccup core     page-helpers]
        [mulk.benki      util db]
        [clojure.core.match
         :only [match]]
        [clojureql core predicates]
        noir.core)
  (:require [noir.session      :as session]
            [noir.response     :as response]
            [clojure.java.jdbc :as sql])
  (:import [org.jsoup.Jsoup]))


(def page_revisions (table :wiki_page_revisions))
(def pages          (table :wiki_pages))


(defn- html-insert-wikilinks [text]
  (clojure.string/replace
   text
   #"\p{javaUpperCase}+\p{javaLowerCase}+\p{javaUpperCase}+\p{javaLowerCase}+\w+"
   (fn [x] (fmt nil "<a href=\"~a\" class=\"benkilink\">~a</a>" (link :wiki x) x))))


(defn- wikilinkify [tag-soup]
  (let [doc   (org.jsoup.Jsoup/parse tag-soup) ]
    (doseq [node     (into [] (.select doc "*"))
            subnode  (into [] (.childNodes node))]
      (when (instance? org.jsoup.nodes.TextNode subnode)
        (let [new-node (org.jsoup.nodes.Element.
                        (org.jsoup.parser.Tag/valueOf "span")
                        "")]
          (.html new-node (html-insert-wikilinks (.text subnode)))
          (.replaceWith subnode new-node)
          (.unwrap new-node))))
    (-> doc (.select "body") (.html))))

(defn- unwikilinkify [tag-soup]
  (let [doc (org.jsoup.Jsoup/parse tag-soup)]
    (doseq [node (-> doc (.select ".benkilink") (.unwrap))])
    (-> doc (.select "body") (.html))))

(def ^{:private true} wiki-page
  {:head 
    ;; Aloha Editor
   (list
    [:link {:rel "stylesheet"
            :href (resolve-uri "/3rdparty/alohaeditor/aloha/css/aloha.css")}]
    [:script {:type "text/javascript"
              :src (resolve-uri "/3rdparty/alohaeditor/aloha/lib/aloha.js")
              :data-aloha-plugins "common/format,common/highlighteditables,common/list,common/link,common/undo,common/paste,common/block"}]
    ;; Custom wiki page stuff 
    [:script {:type "text/javascript"
              :src (resolve-uri "/js/wiki.js")}])})

(def ^{:private true} plain-page
  {})


(defpage "/wiki" []
  ;; NB. response/redirect calls options/resolve-uri.
  (response/redirect "/wiki/Home"))

(defpage "/wiki/:title" {title :title, revision-id :revision}
  (with-auth
    (let [revisions-with-title (-> page_revisions
                                   (select (where (=* :title title)))
                                   (sort [:date#desc]))
          revision             (if revision-id
                                 (with-dbt (first @(select page_revisions
                                                           (where (=* :id (Integer/parseInt revision-id))))))
                                 (with-dbt (first @revisions-with-title)))]
      (layout wiki-page
              (fmt nil "~A — Benki~@[/~A~] " title revision-id)
              (if revision
                [:div#wiki-page-content (wikilinkify (:content revision))]
                [:div#wiki-page-content [:p "This page does not exist yet."]])
              [:hr]
              [:div#wiki-page-footer {:style "text-align: right"}
               [:a {:href (link :wiki title "/revisions")} "Page revisions"]]))))

(defpage "/wiki/:title/revisions" {title :title}
  (with-auth
    (let [revisions (with-dbt
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
        (layout plain-page
                (fmt nil "Revision list — ~A — Benki" title)
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
               [:td (:title rev)]])]])))))

(defn insert-empty-page []
  (sql/with-query-results results ["INSERT INTO wiki_pages DEFAULT VALUES RETURNING *"]
    (first (into () results))))

(defpage [:post "/wiki/:title"] {title :title, content :content}
  (with-dbt
    (let [revisions (-> page_revisions
                        (select (where (=* :title title)))
                        (sort [:date#desc]))
          revision  (first @revisions)
          page      (:page revision)
          dbcontent (unwikilinkify content)]
      (if-let [user (Integer. (session/get :user))]
        (let [page-id (if page page (:id (insert-empty-page)))]
          (when-not (= (:content revision) dbcontent)  ;skip if content unmodified
            (sql/insert-values
             :wiki_page_revisions
             [:page   :title :content  :author :format]
             [page-id title  dbcontent user   "html5"]))
          {:stetus 200, :headers {}, :body (wikilinkify dbcontent)})
        {:status 403, :headers {}, :body ""}))))
