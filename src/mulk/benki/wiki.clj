(ns mulk.benki.wiki
  (:refer-clojure :exclude [distinct conj! case compile drop take sort disj!
                            resultset-seq])
  (:use [clojure         repl pprint]
        [clojure.contrib error-kit]
        [hiccup core     page-helpers]
        [mulk.benki      util]
        [clojure.core.match.core
         :only [match]]
        [ring.util.response
         :only [redirect]]
        clojureql.core
        [clojure.java.jdbc
         :only [transaction]]
        noir.core)
  (:require mulk.benki.main))


(def page_revisions (table :page_revisions))
(def pages          (table :pages))


(defpage "/wiki" []
  (redirect (resolve-uri "/wiki/Home")))

(defpage "/wiki/:id" {id :id, revision-id :revision}
  (let [page      (-> pages
                      (select (if (number? id)
                                (where (= :id    id))
                                (where (= :title id)))))
        revisions (-> page_revisions
                      (join page (where (= :pages.id :page_revisions.page)))
                      ;;(project [:page_revisions.*])
                      (project page_revisions))
        revision  (if revision-id
                    (select revisions (where (= :id revision-id)))
                    (first (sort revisions [:published#desc])))]
    (layout (fmt nil "~A — Benki~@[/~A~] " id revision-id)
      [:pre (prn-str revision)])))
