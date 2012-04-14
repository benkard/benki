(ns mulk.benki.book_marx
  (:refer-clojure)
  (:use [clojure         repl]
        [hiccup core     page-helpers]
        [clojureql       predicates]
        [clojure.core.match :only [match]]
        [hiccup.core        :only [escape-html]]
        [ring.util.codec    :only [url-encode]]
        noir.core
        [mulk.benki util db auth config webutil feed])
  (:require [clojure.algo.monads  :as m]
            [clojure.java.jdbc    :as sql]
            [clojure.string       :as string]
            [clojureql.core       :as cq]
            [noir.request         :as request]
            [noir.response        :as response]
            [noir.session         :as session]
            hiccup.core)
  (:import [org.jsoup         Jsoup]))


(def bookmark_tags (cq/table :bookmark_tags))
(def bookmarks     (cq/table :bookmarks))
(def tags          (cq/table :tags))
(def users         (cq/table :users))


(def bookmarx-list-page
  {:head (list
          [:link {:rel "stylesheet"
                  :href (resolve-uri "/style/hammer-and-sickle.css")
                  :type "text/css"}]
          [:link {:rel "stylesheet"
                  :href (resolve-uri "/style/bookmarx.css")
                  :type "text/css"}])})
(def bookmarx-submission-page
  {:head   (list
            [:link {:rel "stylesheet"
                    :href (resolve-uri "/style/hammer-and-sickle.css")
                    :type "text/css"}]
            [:link {:rel "stylesheet"
                    :href (resolve-uri "/3rdparty/jquery-ui/css/ui-lightness/jquery-ui-1.8.18.custom.css")
                    :type "text/css"}])
   :bottom (list
            [:script {:type "text/javascript"
                      :src (resolve-uri "/3rdparty/jquery-ui/js/jquery-ui-1.8.18.custom.min.js")}]
            [:script {:type "text/javascript"
                      :src (resolve-uri "/js/bookmarx-submit.js")}])})

(defn restrict-visibility [table user]
  (if user
    (cq/select table
               (cq/where (or (=* :visibility "public")
                             (=* :visibility "protected")
                             (and (=* :visibility "private")
                                  (=* :owner      user)))))
    (cq/select table
               (cq/where (=* :visibility "public")))))



(defn htmlize-description [text]
  (letfn [(listify [par]
            (when (re-matches #"^(?msu)\s*\*\s+.*" par)
              [:ul {}
               (map (fn [item] [:li {} item])
                    (filter #(not (= "" (string/trim %)))
                            (string/split par #"(?su)(\n|^)\s*\*\s+")))]))]
    (let [input (escape-html text)]
      (map (fn [par]
             (or (listify par)
                 [:p {} par]))
           (string/split input #"\n\s*?\n")))))

(defn bookmarks-visible-by [user]
  (-> bookmarks
      (cq/join users (=* :bookmarks.owner :users.id))
      (cq/project [:bookmarks.* :users.first_name :users.last_name])
      ;;(cq/rename {:users.id :uid})
      (restrict-visibility user)
      (cq/sort [:date#desc])))

(defpage "/marx" {}
  (let [marks (bookmarks-visible-by *user*)]
    (with-dbt
      (layout bookmarx-list-page "Book Marx"
        [:div {:id "notifications"
               :class "notifications"}
         (login-message)]
        [:div
         ;;(.toString marks)
         [:ul {:class "bookmarx-list"}
          (for [mark @marks]
            [:li {:class "bookmark"}
             [:h2 {:class "bookmark-title"}
              [:a {:href (escape-html (:uri mark))}
               (escape-html (:title mark))]]
             [:p {:class "bookmark-date-and-owner"}
              [:span {:class "bookmark-date"}
               (escape-html (format-date (:date mark)))]
              [:span {:class "bookmark-owner"} " by " (escape-html (:first_name mark))]]
             [:div {:class "bookmark-description"}
              (htmlize-description (:description mark))]])]]
        [:div {:id "bookmarx-footer"}
         (let [feed-link (linkrel :marx :feed)]
           [:span {:id "bookmarx-footer-text"}
            "[" [:a {:href (resolve-uri feed-link)} "Atom"] "]"
            (when *user*
              (list
               " [" [:a {:href (resolve-uri (authlink feed-link))} "Atom auth"] "]"
               " [" [:a {:href (authlink (:uri (request/ring-request)))} "authlink"] "]"))])]))))

(defn marx-feed-for-user [user]
  (let [marks (bookmarks-visible-by user)]
    (with-dbt
      (let [last-updated (sql/with-query-results results
                           ["SELECT MAX(date) AS maxdate FROM bookmarks"]
                           (:maxdate (first results)))
            items  (map #(with-meta
                           (assoc %
                             (hiccup.core/html (htmlize-description (:description %))))
                           {:type ::bookmark}) @marks)]
        (generate-feed "Book Marx" last-updated "marx" (link :marx)
                       items)))))

(defpage "/marx/feed" {}
  (response/content-type "application/atom+xml; charset=UTF-8"
    (marx-feed-for-user *user*)))

(defpage "/marx/tags" {}
  (with-auth
    (with-dbt
      (sql/with-query-results tags ["SELECT DISTINCT tag FROM bookmark_tags ORDER BY tag ASC"]
        (response/json (doall (map :tag tags)))))))

(defmacro ignore-errors [& body]
  `(try (do ~@body)
     (catch java.lang.Exception e#
       nil)))

(defpage [:get "/marx/submit"] {uri :uri, description :description, origin :origin, title :title}
  (with-auth
    (let [title (or title
                    (m/domonad m/maybe-m
                      ;; FIXME: Using slurp here is a potential security problem
                      ;; because it permits access to internal resources!
                      [:when  uri
                       :when  (or (.startsWith uri "http://")
                                  (.startsWith uri "https://"))
                       soup   (ignore-errors (slurp uri))
                       page   (Jsoup/parse soup)
                       title  (.select page "title")]
                      (.text title)))
          origin (or origin
                     (get-in (request/ring-request) [:headers "Referer"])
                     uri)]
      (layout bookmarx-submission-page "Submit New Bookmark"
        [:form {:method "POST", :action (link :marx :submit)}
         [:table
          [:tr [:td "URI: "]         [:td [:input {:type "text", :name "uri", :size 100, :value uri}]]]
          [:tr [:td "Title: "]       [:td [:input {:type "text", :name "title", :size 100, :value title}]]]
          [:tr [:td "Description: "] [:td [:textarea {:name "description", :rows 25, :cols 100}]]]
          [:tr [:td "Tags: "]        [:td [:input {:type "text", :name "tags", :size 100, :id "bookmark-tags-field"}]]]
          [:tr
           [:td "Visibility: "]
           [:td
            [:input {:type "radio", :name "visibility", :value "private"}
             "Private"]
            [:input {:type "radio", :name "visibility", :value "protected",
                     :checked "checked"}
             "Semi-private"]
            [:input {:type "radio", :name "visibility", :value "public"}
             "Public"]]]]
         [:input {:type "hidden", :name "origin", :value origin}]
         [:input {:type "submit"}]]))))

(defpage [:post "/marx/submit"] {uri :uri, description :description,
                                 title :title, tags :tags, visibility :visibility,
                                 origin :origin}
  (with-auth
    (let [tagseq (map string/trim (string/split tags #","))]
      (with-dbt
        (let [bookmark (sql/with-query-results
                         results
                         ["INSERT INTO bookmarks (owner, uri, title, description,
                                                  visibility)
                              VALUES (?, ?, ?, ?, ?)
                           RETURNING id"
                          *user* uri title description visibility]
                         (:id (first (into () results))))]
          (doseq [tag tagseq]
            (sql/insert-values :bookmark_tags [:bookmark :tag] [bookmark tag]))))))
  (if (and origin (not= origin ""))
    (redirect origin)
    (redirect (link :marx))))


;; (defpage "/users/:id/marx" {user :id}
;;   (let [user (session/get :user)
;;         marks (-> bookmarks
;;                   (cq/join users (cqp/=* :tags.owner user))
;;                   (sort [:date#desc]))]
;;     (with-dbt
;;       @marks)))
